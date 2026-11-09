package com.mcatk.medalcabinet;

import com.mcatk.medalcabinet.command.MedalAdminCmd;
import com.mcatk.medalcabinet.command.MedalShowCmd;
import com.mcatk.medalcabinet.command.MedalUsualCmd;
import com.mcatk.medalcabinet.listener.MedalPlayerListener;
import com.mcatk.medalcabinet.papi.MedalPapi;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MedalCabinet extends JavaPlugin {

    private static MedalCabinet plugin;

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
