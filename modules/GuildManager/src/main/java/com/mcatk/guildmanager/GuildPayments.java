package com.mcatk.guildmanager;

import com.mcatk.guildmanager.core.repository.jdbc.GuildTransactionException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/** Vault calls are confined to the server thread; SQL and receipts are confined to a worker. */
public final class GuildPayments implements AutoCloseable {
    private final GuildManager plugin;
    private final BukkitScheduler scheduler;
    private final ExecutorService worker;
    private final Map<UUID, Payment> pending = new ConcurrentHashMap<>();
    private final Set<String> reserved = ConcurrentHashMap.newKeySet();
    private final Set<String> unresolved = ConcurrentHashMap.newKeySet();
    private final Path receipts;
    private volatile boolean closing;
    private boolean tablesValidated;

    private static final class Payment {
        final String id = UUID.randomUUID().toString();
        final Player player;
        final UUID playerId;
        final String name, key;
        final int amount;
        volatile boolean charged, needsRefund, uncertain, debitStarted, debitKnown;
        final AtomicBoolean refundStarted = new AtomicBoolean();
        Payment(Player player, String key, int amount) {
            this.player = player; this.playerId = player.getUniqueId(); this.name = player.getName();
            this.key = key; this.amount = amount;
        }
    }

    public GuildPayments(GuildManager plugin) {
        this.plugin = plugin;
        scheduler = Bukkit.getScheduler();
        receipts = plugin.getDataFolder().toPath().resolve("pending-payments");
        worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(64), r -> {
            Thread thread = new Thread(r, "GuildManager-Payments"); thread.setDaemon(true); return thread;
        });
        File[] files = receipts.toFile().listFiles((dir, name) -> name.endsWith(".properties"));
        if (files != null) for (File file : files) {
            try (InputStream input = new FileInputStream(file)) {
                Properties record = new Properties(); record.load(input);
                unresolved.add(record.getProperty("player", "").toLowerCase(Locale.ROOT));
                plugin.getLogger().severe("Unresolved guild payment requires reconciliation: " + file.getName());
            } catch (IOException e) { throw new IllegalStateException("Cannot read guild payment receipts", e); }
        }
    }

    public void charge(Player player, String key, int amount, BooleanSupplier check, Runnable commit, String success) {
        if (closing || !player.isOnline() || amount <= 0) return;
        if (unresolved.contains(player.getName().toLowerCase(Locale.ROOT))) {
            player.sendMessage("§c你有一笔待核对交易，请联系管理员"); return;
        }
        Payment payment = new Payment(player, key, amount);
        if (pending.size() >= 16 || !reserved.add(key)) {
            player.sendMessage("§e相关操作正在处理，请稍候"); return;
        }
        if (pending.putIfAbsent(payment.playerId, payment) != null) {
            reserved.remove(key); player.sendMessage("§e你的操作正在处理，请稍候"); return;
        }
        worker.execute(() -> {
            if (closing) { finish(payment); return; }
            try {
                if (!tablesValidated) { plugin.getGuildService().validatePayments(); tablesValidated = true; }
                if (!check.getAsBoolean()) {
                    finish(payment); onMain(() -> player.sendMessage("§c公会 ID 已存在或玩家状态已变化，未扣款")); return;
                }
                write(payment, "PREPARED");
            } catch (Exception e) {
                finish(payment); plugin.getLogger().log(Level.WARNING, "Guild payment not charged", e);
                onMain(() -> player.sendMessage("§c数据库暂不可用，未扣款")); return;
            }
            onMain(() -> {
                if (closing) return;
                if (!player.isOnline()) { worker.execute(() -> finish(payment)); return; }
                try {
                    payment.debitStarted = true;
                    payment.charged = plugin.takePlayerMoney(player, amount);
                    payment.debitKnown = true;
                    if (!payment.charged) {
                        worker.execute(() -> finish(payment)); player.sendMessage("§cAC点不足"); return;
                    }
                } catch (RuntimeException error) {
                    payment.uncertain = true;
                    worker.execute(() -> review(payment, "DEBIT_UNKNOWN", error));
                    return;
                }
                worker.execute(() -> {
                    try { write(payment, "CHARGED"); }
                    catch (IOException e) { refundRequired(payment); return; }
                    try { commit.run(); }
                    catch (GuildTransactionException e) {
                        if (e.isOutcomeUnknown()) review(payment, "COMMIT_UNKNOWN", e);
                        else refundRequired(payment);
                        return;
                    } catch (RuntimeException e) { review(payment, "OUTCOME_UNKNOWN", e); return; }
                    finish(payment);
                    try { plugin.getGuildService().refresh(); }
                    catch (RuntimeException e) { plugin.getLogger().log(Level.WARNING, "Committed guild payment; cache refresh will retry", e); }
                    onMain(() -> { if (player.isOnline()) player.sendMessage(success); });
                });
            });
        });
    }

    private void refundRequired(Payment payment) {
        payment.needsRefund = true;
        try { write(payment, "REFUND_REQUIRED"); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Cannot update payment receipt " + payment.id, e); }
        onMain(() -> refund(payment));
    }

    private void refund(Payment payment) {
        if (!payment.refundStarted.compareAndSet(false, true)) return;
        try {
            if (!plugin.refundPlayerMoney(payment.player, payment.amount)) throw new IllegalStateException("Vault refund failed");
            Runnable cleanup = () -> finish(payment);
            if (closing) cleanup.run(); else worker.execute(cleanup);
            if (payment.player.isOnline()) payment.player.sendMessage("§e操作失败，已退回 " + payment.amount + " AC点");
        } catch (RuntimeException e) {
            Runnable review = () -> review(payment, "REFUND_UNKNOWN", e);
            if (closing) review.run(); else worker.execute(review);
        }
    }

    private void write(Payment payment, String state) throws IOException {
        Files.createDirectories(receipts);
        Properties record = new Properties(); record.setProperty("player", payment.name);
        record.setProperty("amount", Integer.toString(payment.amount)); record.setProperty("operation", payment.key);
        record.setProperty("state", state); record.setProperty("id", payment.id);
        Path temp = receipts.resolve(payment.id + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) { record.store(output, "Reconcile Vault and MySQL before retrying"); }
        Files.move(temp, receipts.resolve(payment.id + ".properties"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private void finish(Payment payment) {
        pending.remove(payment.playerId, payment); reserved.remove(payment.key);
        try { Files.deleteIfExists(receipts.resolve(payment.id + ".properties")); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Cannot remove completed payment receipt " + payment.id, e); }
    }

    private void review(Payment payment, String state, Exception error) {
        unresolved.add(payment.name.toLowerCase(Locale.ROOT));
        try { write(payment, state); } catch (IOException e) { error.addSuppressed(e); }
        pending.remove(payment.playerId, payment); reserved.remove(payment.key);
        plugin.getLogger().log(Level.SEVERE, state + " payment=" + payment.id + " player=" + payment.name + " AC=" + payment.amount, error);
        onMain(() -> { if (payment.player.isOnline()) payment.player.sendMessage("§c交易结果待核对，请联系管理员，勿重复付款"); });
    }

    private void onMain(Runnable action) {
        if (!closing && plugin.isEnabled()) scheduler.runTask(plugin, action);
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true; worker.shutdown();
        boolean complete = false;
        try { complete = worker.awaitTermination(30, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        for (Payment payment : pending.values()) {
            if (payment.needsRefund) refund(payment);
            else if (!payment.charged && !payment.uncertain && (!payment.debitStarted || payment.debitKnown)) finish(payment);
            else plugin.getLogger().severe("Retain payment receipt " + payment.id + ": outcome not confirmed; worker complete=" + complete);
        }
    }
}
