package com.mcatk.itemmanager;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class ItemSort {
    private final HashMap<String, Items> itemsHashMap;

    public ItemSort() {
        itemsHashMap = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder("物品类已载入:\n");
        for (String key : ItemManager.getPlugin().getConfig().getKeys(false)) {
            itemsHashMap.put(key, new Items(key));
            stringBuilder.append(key).append("\n");
        }
        ItemManager.getPlugin().getLogger().info(stringBuilder.toString());
    }

    public void createSort(String id) {
        itemsHashMap.put(id, new Items(id));
    }

    public void addItem(String id1, String id2, ItemStack itemStack) {
        if (!itemsHashMap.containsKey(id1)) {
            itemsHashMap.put(id1, new Items(id1));
        }
        itemsHashMap.get(id1).getItemStackHashMap().put(id2, itemStack);
        ItemManager.getPlugin().getConfig().set(id1 + "." + id2, itemStack);
        ItemManager.getPlugin().saveConfig();
    }

    public ItemStack getItem(String id1, String id2) {
        Items items = itemsHashMap.get(id1);
        if (items == null) {
            return null;
        }
        return items.getItemStackHashMap().get(id2);
    }

    public String listAll() {
        StringBuilder stringBuilder = new StringBuilder("ItemManager-List:\n");
        for (Items items : itemsHashMap.values()) {
            stringBuilder.append("§2类型(").append(items.getId()).append("):\n");
            for (Map.Entry<String, ItemStack> entry :
                    items.getItemStackHashMap().entrySet()) {
                stringBuilder.append(getItemDisplayName(entry.getValue()))
                        .append("§f(").append(entry.getKey()).append(") ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    private String getItemDisplayName(ItemStack itemStack) {
        if (itemStack == null) {
            return "未知物品";
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            return itemMeta.getDisplayName();
        }
        return itemStack.getType().name();
    }

}
