package com.mcatk.acshop;

import com.mcatk.acshop.commodity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getTitle().contains("§6AC商店-")) {
            if (event.getWhoClicked() instanceof Player) {
                event.setCancelled(true);
                // Security: prevent using items from the player's own inventory to trigger shop actions.
                if (event.getClickedInventory() == null ||
                    !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    return;
                }

                ItemStack icon = event.getCurrentItem();
                if (icon == null || !icon.hasItemMeta()) {
                    return;
                }

                if (icon.getItemMeta().hasDisplayName() &&
                    icon.getItemMeta().getDisplayName().endsWith("返回")) {
                    ((Player) event.getWhoClicked()).chat("/menu_shop");
                    return;
                }

                List<String> list = icon.getItemMeta().getLore();
                if (list != null && !list.isEmpty()) {
                    String[] titleParts = event.getInventory().getTitle().split("-");
                    if (titleParts.length > 1) {
                        String shopId = titleParts[1];
                        String[] loreParts = list.get(0).split(":");
                        if (loreParts.length > 1) {
                            String itemId = loreParts[1];
                            Item item = AcShop.getShops().getItem(shopId, itemId);
                            new Operation().buy((Player) event.getWhoClicked(), item);
                        }
                    }
                }
            }
        }
    }
    
}
