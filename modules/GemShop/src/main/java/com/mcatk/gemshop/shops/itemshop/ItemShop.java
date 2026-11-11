package com.mcatk.gemshop.shops.itemshop;

import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.Message;
import com.mcatk.gemshop.InventoryDelivery;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class ItemShop {
    private HashMap<String, Items> itemsMap;
    
    public ItemShop() {
        itemsMap = new HashMap<>();
        ConfigurationSection cs =
                GemShop.getPlugin().getConfig().getConfigurationSection("Items");
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                itemsMap.put(key, new Items(key));
            }
        }
    }
    
    public void buyItem(Player player, String shopId, String itemId) {
        Items items = itemsMap.get(shopId);
        if (items == null) {
            player.sendMessage(Message.ERROR + "无该分类");
            return;
        }
        Item item = items.getMap().get(itemId);
        if (item == null) {
            player.sendMessage(Message.ERROR + "无该商品");
            return;
        }
        ItemStack delivery = item.getItemStack().clone();
        GemShop.getPlugin().getPurchases().buy(player, item.getPrice(), () -> {
            boolean fits = InventoryDelivery.canFit(player.getInventory(), delivery);
            if (!fits) player.sendMessage("§c背包空间不足");
            return fits;
        }, () -> InventoryDelivery.deliver(player.getInventory(), delivery));
    }
    

    public void addItem(Player player, String shopId, String itemId, String price) {
        Item item = new Item(itemId, player.getInventory().getItemInMainHand(),
                Integer.parseInt(price));
        if (itemsMap.get(shopId) == null) {
            itemsMap.put(shopId, new Items(shopId));
        }
        itemsMap.get(shopId).getMap().put(itemId, item);
        GemShop.getPlugin().getConfig().set("Items." + shopId + "." + itemId, item);
        GemShop.getPlugin().saveConfigAsync();
        player.sendMessage(Message.INFO + "添加成功：" + itemId + " " + price + "宝石");
    }
    
    public void delItem(String shopId, String itemId) {
        Items items = itemsMap.get(shopId);
        if (items == null) {
            return;
        }
        items.getMap().remove(itemId);
        GemShop.getPlugin().getConfig().set("Items." + shopId + "." + itemId, null);
        GemShop.getPlugin().saveConfigAsync();
    }
    
    public Items getItems(String shopId) {
        return itemsMap.get(shopId);
    }
    
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("现已有商店：");
        for (String key: itemsMap.keySet()){
            stringBuilder.append(key);
        }
        return stringBuilder.toString();
    }
}
