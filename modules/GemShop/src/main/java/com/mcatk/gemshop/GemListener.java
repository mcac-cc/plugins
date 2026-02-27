package com.mcatk.gemshop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GemListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getTitle().contains("§6宝石商店-")) {
            if (event.getWhoClicked() instanceof Player) {
                event.setCancelled(true);
                ItemStack icon = event.getCurrentItem();

                if (icon == null || !icon.hasItemMeta()) {
                    return;
                }

                ItemMeta meta = icon.getItemMeta();

                if (meta.hasDisplayName() && meta.getDisplayName().endsWith("返回")){
                    ((Player) event.getWhoClicked()).chat("/gemshop");
                    return;
                }

                List<String> list = meta.getLore();
                if (list != null && !list.isEmpty()) {
                    String[] titleParts = event.getInventory().getTitle().split("-");
                    if (titleParts.length > 1) {
                        String shopId = titleParts[1];
                        String[] loreParts = list.get(0).split(":");
                        if (loreParts.length > 1) {
                            String itemId = loreParts[1];
                            GemShop.getPlugin().getShopFactory().
                                    getItemShop().buyItem(
                                            (Player) event.getWhoClicked(),shopId, itemId
                            );
                        }
                    }
                }
            }
        }
    }
}
