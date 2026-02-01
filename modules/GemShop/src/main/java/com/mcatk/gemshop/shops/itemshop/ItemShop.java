package com.mcatk.gemshop.shops.itemshop;

import com.mcatk.gem.Gem;
import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.Message;
import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ItemShop {
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "GemShop-IO-Thread");
            t.setDaemon(true);
            return t;
        }
    });

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
                if (itemsMap.get(shopId) == null) {
                    player.sendMessage(Message.ERROR + "无该分类");
                }
                if (!itemsMap.get(shopId).getMap().containsKey(itemId)) {
                    player.sendMessage(Message.ERROR + "无该商品");
                } else if (
                        Gem.getPlugin().getGemExecutor().takeGems(
                                player.getName(),
                                itemsMap.get(shopId).getMap().get(itemId).getPrice()
                        )
                ) {
                    player.getInventory().addItem(
                            itemsMap.get(shopId).getMap().get(itemId).getItemStack()
                    );
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
        saveConfigAsync();
        player.sendMessage(Message.INFO + "添加成功：" + itemId + " " + price + "宝石");
    }
    
    public void delItem(String shopId, String itemId) {
        itemsMap.get(shopId).getMap().remove(itemId);
        GemShop.getPlugin().getConfig().set("Items." + shopId + "." + itemId, null);
        saveConfigAsync();
    }

    private void saveConfigAsync() {
        if (!(GemShop.getPlugin().getConfig() instanceof YamlConfiguration)) {
            GemShop.getPlugin().saveConfig();
            return;
        }

        YamlConfiguration yaml = (YamlConfiguration) GemShop.getPlugin().getConfig();
        final String data = yaml.saveToString();
        final File file = new File(GemShop.getPlugin().getDataFolder(), "config.yml");

        ioExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (file.getParentFile() != null && !file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    GemShop.getPlugin().getLogger().severe("Could not save config.yml asynchronously!");
                    e.printStackTrace();
                }
            }
        });
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
