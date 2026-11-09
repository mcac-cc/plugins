package com.mcatk.gemshop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Called on the server thread. Capacity checks include partially filled stacks. */
public final class InventoryDelivery {
    private InventoryDelivery() { }

    public static boolean canFit(PlayerInventory inventory, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return false;
        int remaining = item.getAmount();
        int maximum = Math.min(item.getMaxStackSize(), inventory.getMaxStackSize());
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) remaining -= maximum;
            else if (slot.isSimilar(item)) remaining -= Math.max(0, maximum - slot.getAmount());
            if (remaining <= 0) return true;
        }
        return false;
    }

    public static boolean deliver(PlayerInventory inventory, ItemStack item) {
        if (!canFit(inventory, item)) return false;
        ItemStack[] before = inventory.getStorageContents().clone();
        for (int i = 0; i < before.length; i++) {
            if (before[i] != null) before[i] = before[i].clone();
        }
        try {
            if (inventory.addItem(item.clone()).isEmpty()) return true;
        } catch (RuntimeException failure) {
            // Restore any partial insertion before reporting a refundable failure.
        }
        inventory.setStorageContents(before);
        return false;
    }
}
