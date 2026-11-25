package com.mcatk.acshop;

import com.mcatk.acshop.commodity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopListener implements Listener {
    private static final String SHOP_MARKER = "  §6AC商店-";
    private static final String ITEM_PREFIX = "商品ID:";

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getInventory().getTitle();
        int marker = title.indexOf(SHOP_MARKER);
        if (marker >= 0) {
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

                if (event.getRawSlot() == ShopGui.BACK_SLOT) {
                    ((Player) event.getWhoClicked()).chat("/menu_shop");
                    return;
                }

                List<String> list = icon.getItemMeta().getLore();
                if (list != null && !list.isEmpty() && list.get(0).startsWith(ITEM_PREFIX)) {
                    String shopId = title.substring(marker + SHOP_MARKER.length());
                    String itemId = list.get(0).substring(ITEM_PREFIX.length());
                    Item item = AcShop.getShops().getItem(shopId, itemId);
                    new Operation().buy((Player) event.getWhoClicked(), item);
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getTitle().contains(SHOP_MARKER)
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getInventory().getSize())) {
            event.setCancelled(true);
        }
    }
}
