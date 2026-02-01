package com.mcatk.itemmanager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class ItemManager extends JavaPlugin {
    
    private static ItemManager plugin;
    private static ItemSort itemSort;
    private ExecutorService ioExecutor;
    
    @Override
    public void onEnable() {
        plugin = this;
        ioExecutor = Executors.newSingleThreadExecutor();
        saveConfig();
        itemSort = new ItemSort();
        regCommand();
        
    }
    
    @Override
    public void onDisable() {
        if (ioExecutor != null) {
            ioExecutor.shutdown();
        }
    }
    
    public static ItemManager getPlugin() {
        return plugin;
    }
    
    public static ItemSort getItemSort() {
        return itemSort;
    }
    
    public static void reload() {
        itemSort = new ItemSort();
    }
    
    private void regCommand() {
        Bukkit.getPluginCommand("im").
                setExecutor(new ItemManagerCommand());
    }
    
    public static ItemStack getItem(String sortId, String itemId) {
        return itemSort.getItem(sortId, itemId);
    }
    
    public static void addItem(String sortId, String itemId, ItemStack itemStack) {
        itemSort.addItem(sortId, itemId, itemStack);
    }

    public void setIoExecutor(ExecutorService ioExecutor) {
        this.ioExecutor = ioExecutor;
    }

    public void saveConfigAsync() {
        if (!isEnabled()) {
            return;
        }
        if (getConfig() instanceof YamlConfiguration) {
            final String data = ((YamlConfiguration) getConfig()).saveToString();
            final File file = new File(getDataFolder(), "config.yml");
            ioExecutor.submit(() -> {
                try {
                    Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Could not save config to " + file, ex);
                }
            });
        } else {
            saveConfig();
        }
    }
}
