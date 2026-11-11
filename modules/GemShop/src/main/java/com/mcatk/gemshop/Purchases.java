package com.mcatk.gemshop;

import com.mcatk.gem.Gem;
import com.mcatk.gem.GemExecutor;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/** All inventory/permission work runs on the server thread; the worker only accesses SQL/files. */
public final class Purchases implements AutoCloseable {
    private final GemShop plugin;
    private final GemExecutor gems;
    private final BukkitScheduler scheduler;
    private final ExecutorService worker;
    private final Map<UUID, Request> pending = new ConcurrentHashMap<>();
    private final Set<String> unresolved = ConcurrentHashMap.newKeySet();
    private final Path receipts;
    private volatile boolean closing;

    private static final class Request {
        final String id = UUID.randomUUID().toString();
        final Player player;
        final UUID playerId;
        final String name;
        final int price;
        final Product product;
        volatile boolean charged, debitStarted, debitKnown;
        final AtomicBoolean settled = new AtomicBoolean();
        Request(Player player, int price, Product product) {
            this.product = product;
            this.player = player; this.playerId = player.getUniqueId(); this.name = player.getName(); this.price = price;
        }
    }

    public Purchases(GemShop plugin) {
        this(plugin, Gem.getPlugin().getGemExecutor());
    }

    Purchases(GemShop plugin, GemExecutor gems) {
        this.plugin = plugin;
        this.gems = gems;
        scheduler = Bukkit.getScheduler();
        receipts = plugin.getDataFolder().toPath().resolve("pending-purchases");
        worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(64), r -> {
            Thread thread = new Thread(r, "GemShop-Purchases"); thread.setDaemon(true); return thread;
        });
        File[] existing = receipts.toFile().listFiles((dir, name) -> name.endsWith(".properties"));
        if (existing != null) for (File file : existing) {
            try (InputStream input = new FileInputStream(file)) {
                Properties record = new Properties(); record.load(input);
                unresolved.add(record.getProperty("player", "").toLowerCase(Locale.ROOT));
                plugin.getLogger().severe("Unresolved purchase requires reconciliation: " + file.getName());
            } catch (IOException e) { throw new IllegalStateException("Cannot read purchase receipts", e); }
        }
    }

    public void buy(Player player, int price, Product product, BooleanSupplier eligible, BooleanSupplier deliver) {
        if (closing || !player.isOnline()) return;
        if (!gems.isReady()) { player.sendMessage("§e宝石服务正在初始化，请稍候，未扣款"); return; }
        if (price <= 0 || !eligible.getAsBoolean()) return;
        if (unresolved.contains(player.getName().toLowerCase(Locale.ROOT))) {
            player.sendMessage("§c你有一笔待核对交易，请联系管理员"); return;
        }
        Request request = new Request(player, price, product);
        if (pending.size() >= 16 || pending.putIfAbsent(request.playerId, request) != null) {
            player.sendMessage("§e交易处理中，请稍候"); return;
        }
        worker.execute(() -> {
            if (closing) { finish(request); return; }
            try {
                write(request, "DEBIT_PENDING");
            } catch (IOException e) { failBeforeCharge(request, e); return; }
            try {
                request.debitStarted = true;
                request.charged = gems.takeGems(request.name, price);
                request.debitKnown = true;
            } catch (RuntimeException e) { review(request, "DEBIT_UNKNOWN", e); return; }
            if (!request.charged) {
                finish(request);
                onMain(() -> player.sendMessage("§c宝石不足"));
                return;
            }
            if (closing) { refund(request); return; }
            try { write(request, "CHARGED"); }
            catch (IOException e) { refund(request); return; }
            onMain(() -> {
                if (closing || !request.settled.compareAndSet(false, true)) return;
                try {
                    if (!player.isOnline() || !eligible.getAsBoolean() || !deliver.getAsBoolean()) {
                        worker.execute(() -> refund(request));
                    } else {
                        player.sendMessage("§a购买成功");
                        worker.execute(() -> finish(request));
                    }
                } catch (RuntimeException e) { worker.execute(() -> review(request, "DELIVERY_UNKNOWN", e)); }
            });
        });
    }

    private void refund(Request request) {
        try {
            write(request, "REFUND_PENDING");
            if (!gems.refundGems(request.name, request.price)) {
                throw new IllegalStateException("Refund account missing");
            }
            finish(request);
            onMain(() -> { if (request.player.isOnline()) request.player.sendMessage("§e未交付商品，已退回宝石"); });
        } catch (Exception e) { review(request, "REFUND_UNKNOWN", e); }
    }

    private void write(Request request, String state) throws IOException {
        Files.createDirectories(receipts);
        Properties record = new Properties();
        request.product.writeTo(record);
        record.setProperty("player", request.name); record.setProperty("amount", Integer.toString(request.price));
        record.setProperty("state", state); record.setProperty("id", request.id);
        Path temp = receipts.resolve(request.id + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) { record.store(output, "Reconcile before retrying; do not blindly refund"); }
        Files.move(temp, receipts.resolve(request.id + ".properties"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private void finish(Request request) {
        pending.remove(request.playerId, request);
        try { Files.deleteIfExists(receipts.resolve(request.id + ".properties")); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Cannot remove completed purchase receipt " + request.id, e); }
    }

    private void review(Request request, String state, Exception error) {
        unresolved.add(request.name.toLowerCase(Locale.ROOT));
        try { write(request, state); }
        catch (IOException e) { error.addSuppressed(e); }
        pending.remove(request.playerId, request);
        plugin.getLogger().log(Level.SEVERE, state + " purchase=" + request.id + " player=" + request.name + " gems=" + request.price, error);
        onMain(() -> { if (request.player.isOnline()) request.player.sendMessage("§c交易结果待核对，请联系管理员，勿重复购买"); });
    }

    private void failBeforeCharge(Request request, Exception error) {
        finish(request);
        plugin.getLogger().log(Level.WARNING, "Purchase not charged", error);
        onMain(() -> request.player.sendMessage("§c交易暂不可用，未扣款"));
    }

    private void onMain(Runnable action) {
        if (!closing && plugin.isEnabled()) scheduler.runTask(plugin, action);
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        // Queued debits see closing and do not charge; already charged callbacks are compensated here.
        worker.execute(() -> {
            for (Request request : pending.values()) {
                if (request.charged && request.settled.compareAndSet(false, true)) refund(request);
                else if (!request.charged && (!request.debitStarted || request.debitKnown)) finish(request);
                else if (!request.charged) plugin.getLogger().severe("Retain purchase receipt " + request.id + ": debit outcome unknown");
            }
        });
        worker.shutdown();
        try {
            if (!worker.awaitTermination(30, TimeUnit.SECONDS)) {
                plugin.getLogger().severe("Purchase shutdown incomplete; retain pending-purchases receipts for reconciliation");
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
