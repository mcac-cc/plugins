package com.mcatk.gemshop;

import com.mcatk.gemshop.commands.CommandGemShop;
import com.mcatk.gemshop.commands.CommandGsi;
import com.mcatk.gemshop.shops.ShopFactory;
import com.mcatk.gemshop.shops.itemshop.Item;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class GemShop extends JavaPlugin {
    private static GemShop plugin;
    private ShopFactory shopFactory;
    private ExecutorService configSaveExecutor;
    
    public static GemShop getPlugin() {
        return plugin;
    }
    
    public ShopFactory getShopFactory() {
        return shopFactory;
    }
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(Item.class);
        plugin = this;
        configSaveExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "GemShop-ConfigSaveThread");
                t.setDaemon(true);
                return t;
            }
        });
        shopFactory = new ShopFactory();
        Bukkit.getPluginCommand("gemshop").
                setExecutor(new CommandGemShop());
        Bukkit.getPluginCommand("gsi").
                setExecutor(new CommandGsi());
        Bukkit.getPluginManager().
                registerEvents(new GemListener(), this);
        log("GemShop已启动");
    }
    
    @Override
    public void onDisable() {
        if (configSaveExecutor != null) {
            configSaveExecutor.shutdown();
            try {
                if (!configSaveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    configSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                configSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log("GemShop已关闭");
    }
    
    public void saveConfigAsync() {
        String configData = getConfig().saveToString();
        configSaveExecutor.submit(() -> {
            File configFile = new File(getDataFolder(), "config.yml");
            try {
                if (!configFile.exists()) {
                    configFile.getParentFile().mkdirs();
                }
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), Charsets.UTF_8)) {
                    writer.write(configData);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void log(String str) {
        getPlugin().getLogger().info(str);
    }
    
}
