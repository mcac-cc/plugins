package com.mcatk.gemshop.shops.itemshop;

import com.mcatk.gem.Gem;
import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.Message;
import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
//        new BukkitRunnable() {
//            @Override
//            public void run() {
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
                if (Gem.getPlugin().getGemExecutor().takeGems(player.getName(), item.getPrice())) {
                    player.getInventory().addItem(item.getItemStack());
                    player.sendMessage(Message.INFO + "购买成功");
                } else {
                    player.sendMessage(Message.INFO + "宝石不足");
                }
//            }
//        }.runTaskAsynchronously(Gem.getPlugin());
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
