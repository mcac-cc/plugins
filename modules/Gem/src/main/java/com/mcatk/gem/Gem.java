package com.mcatk.gem;

import com.mcatk.gem.command.CommandGem;
import com.mcatk.gem.command.CommandGems;
import com.mcatk.gem.papi.GemPapi;
import com.mcatk.gem.sql.MySQLManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class Gem extends JavaPlugin implements Listener {
    
    private static Gem plugin;
    private GemExecutor gemExecutor;
    private FileLog fileLog;
    private final Map<String, Integer> balances = new ConcurrentHashMap<>();
    private final Map<String, Long> refreshed = new ConcurrentHashMap<>();
    private final Set<String> loading = ConcurrentHashMap.newKeySet();
    
    public static Gem getPlugin() {
        return plugin;
    }
    
    public GemExecutor getGemExecutor() {
        return gemExecutor;
    }
    
    @Override
    public void onEnable() {
        plugin = this;
        gemExecutor = new GemExecutor();
        fileLog = new FileLog();
        //配置文件
        saveDefaultConfig();
        reloadConfig();
        //连接MySQL
        new BukkitRunnable() {
            @Override
            public void run() {
                MySQLManager.getInstance().enableMySQL();
                getLogger().info("连接到MySQL");
            }
        }.runTaskAsynchronously(this);
        //注册指令
        Bukkit.getPluginCommand("gem").setExecutor(new CommandGem());
        Bukkit.getPluginCommand("gems").setExecutor(new CommandGems());
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("启动完毕");
        //PAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("检测到PlaceholderAPI，已启动PAPI变量");
            new GemPapi(this).register();
        }
    }
    
    @Override
    public void onDisable() {
        MySQLManager.getInstance().shutdown();
    }
    
    public <T> void queryAsync(Supplier<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        org.bukkit.scheduler.BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskAsynchronously(this, () -> {
            try {
                T result = work.get();
                if (isEnabled()) scheduler.runTask(this, () -> success.accept(result));
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Gem database operation failed", e);
                if (isEnabled()) scheduler.runTask(this, () -> failure.accept(e));
            }
        });
    }

    public int cachedBalance(String name) {
        long now = System.currentTimeMillis();
        if (now - refreshed.getOrDefault(name, 0L) >= 5000 && loading.add(name)) {
            refreshed.put(name, now);
            queryAsync(() -> gemExecutor.getGems(name), value -> {
                loading.remove(name);
                if (Bukkit.getPlayerExact(name) == null) {
                    balances.remove(name);
                    refreshed.remove(name);
                } else balances.put(name, value);
            }, error -> loading.remove(name));
        }
        return balances.getOrDefault(name, 0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String name = event.getPlayer().getName();
        balances.remove(name);
        refreshed.remove(name);
    }

    public void invalidateBalance(String name) {
        refreshed.remove(name);
    }

    public void log(String s) {
        String log = new SimpleDateFormat("[yyyy-MM-dd hh:mm:ss]").format(new Date())
                + s;
        Gem.getPlugin().getLogger().info(log);
        try {
            if (fileLog != null) fileLog.log(log);
        } catch (RuntimeException failure) {
            getLogger().log(Level.WARNING, "Unable to write Gem file log", failure);
        }
    }
    
}
