package com.mcatk.itemmanager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class Items {
    private String id;
    private final HashMap<String, ItemStack> itemStackHashMap;
    
    public Items(String id) {
        this.id = id;
        itemStackHashMap = new HashMap<>();
        ConfigurationSection cs =
                ItemManager.getPlugin().getConfig().getConfigurationSection(id);
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                ItemStack template = cs.getItemStack(key);
                if (template == null) {
                    ItemManager.getPlugin().getLogger().warning("忽略无效物品模板: " + cs.getCurrentPath() + "." + key);
                    continue;
                }
                itemStackHashMap.put(key, template);
            }
        }
    }
    
    public String getId() {
        return id;
    }
    
    public HashMap<String, ItemStack> getItemStackHashMap() {
        return itemStackHashMap;
    }
}
