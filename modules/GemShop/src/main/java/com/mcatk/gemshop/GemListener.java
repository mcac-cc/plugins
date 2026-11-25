package com.mcatk.gemshop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GemListener implements Listener {
    private static final String SHOP_PREFIX = "§6宝石商店-";
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getTitle().startsWith(SHOP_PREFIX)) {
            if (event.getWhoClicked() instanceof Player) {
                event.setCancelled(true);
                // Security: Prevent using items from player's inventory to trigger shop actions
                if (event.getClickedInventory() == null ||
                    !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    return;
                }

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
                if (list != null && !list.isEmpty() && list.get(0).startsWith("ID:")) {
                    String shopId = event.getInventory().getTitle().substring(SHOP_PREFIX.length());
                    String itemId = list.get(0).substring("ID:".length());
                    if (!shopId.isEmpty() && !itemId.isEmpty()) {
                        GemShop.getPlugin().getShopFactory().getItemShop()
                                .buyItem((Player) event.getWhoClicked(), shopId, itemId);
                    }
                }
            }
        }
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getTitle().startsWith(SHOP_PREFIX)
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getInventory().getSize())) {
            event.setCancelled(true);
        }
    }
}
