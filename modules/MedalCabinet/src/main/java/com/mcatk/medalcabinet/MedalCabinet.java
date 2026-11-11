package com.mcatk.medalcabinet;

import com.mcatk.medalcabinet.command.MedalAdminCmd;
import com.mcatk.medalcabinet.command.MedalShowCmd;
import com.mcatk.medalcabinet.command.MedalUsualCmd;
import com.mcatk.medalcabinet.listener.MedalPlayerListener;
import com.mcatk.medalcabinet.papi.MedalPapi;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class MedalCabinet extends JavaPlugin {

    private static MedalCabinet plugin;
    private final Set<String> titleLoads = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> titleRequests = new ConcurrentHashMap<>();

    public static MedalCabinet getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        SQLManager.setInstance(null);
        saveDefaultConfig();
        regCommand();
        regDependency();
        Bukkit.getPluginManager().registerEvents(new MedalPlayerListener(), this);
        getLogger().info("启动成功");
    }

    @Override
    public void onDisable() {
        SQLManager.shutdownInstance();
    }

    public <T> void queryAsync(Supplier<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        org.bukkit.scheduler.BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskAsynchronously(this, () -> {
            try {
                T result = work.get();
                if (isEnabled()) scheduler.runTask(this, () -> success.accept(result));
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Medal database operation failed", e);
                if (isEnabled()) scheduler.runTask(this, () -> failure.accept(e));
            }
        });
    }

    public void requestTitle(String name) {
        SQLManager manager = SQLManager.getInstance();
        if (manager.isMainMedalCached(name)) return;
        long now = System.currentTimeMillis();
        Long last = titleRequests.get(name);
        if (last != null && now - last < 5000 || !titleLoads.add(name)) return;
        titleRequests.put(name, now);
        queryAsync(() -> manager.getMainMedal(name), medal -> {
            titleLoads.remove(name);
            if (Bukkit.getPlayerExact(name) == null) clearTitle(name);
        }, error -> titleLoads.remove(name));
    }

    public void clearTitle(String name) {
        SQLManager.getInstance().clearCache(name);
        titleRequests.remove(name);
    }

    private void regCommand() {
        Bukkit.getPluginCommand("medal").
                setExecutor(new MedalUsualCmd());
        Bukkit.getPluginCommand("medalshow").
                setExecutor(new MedalShowCmd());
        Bukkit.getPluginCommand("medaladmin").
                setExecutor(new MedalAdminCmd());
    }

    private void regDependency() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MedalPapi().register();
            getLogger().info("检测到PlaceholderAPI，已启动PAPI变量");
        }
    }
}
