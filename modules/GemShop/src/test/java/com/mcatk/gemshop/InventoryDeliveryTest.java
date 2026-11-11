package com.mcatk.gemshop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import java.util.HashMap;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InventoryDeliveryTest {
    private ItemStack item(int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE); when(item.getAmount()).thenReturn(amount);
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenAnswer(call -> item);
        return item;
    }
    private PlayerInventory inventory(ItemStack... slots) {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getMaxStackSize()).thenReturn(64);
        when(inventory.getStorageContents()).thenReturn(slots);
        return inventory;
    }
    @Test public void fullInventoryCannotReceiveItems() {
        ItemStack full = item(64), purchase = item(1);
        PlayerInventory inventory = inventory(full);
        assertFalse(InventoryDelivery.deliver(inventory, purchase));
        verify(inventory, never()).addItem(any(ItemStack.class));
    }
    @Test public void capacityIncludesPartialStacksWithoutTreatingUnrelatedItemsAsSpace() {
        ItemStack partial = item(60), purchase = item(4);
        when(partial.isSimilar(purchase)).thenReturn(true);
        PlayerInventory inventory = inventory(partial);
        assertTrue(InventoryDelivery.canFit(inventory, purchase));
        when(purchase.getAmount()).thenReturn(5);
        assertFalse(InventoryDelivery.canFit(inventory, purchase));
    }
    @Test public void leftoverDeliveryRestoresStorageBeforeReportingFailure() {
        ItemStack purchase = item(1);
        PlayerInventory inventory = inventory((ItemStack)null);
        HashMap<Integer,ItemStack> leftovers = new HashMap<>(); leftovers.put(0,purchase);
        when(inventory.addItem(any(ItemStack.class))).thenReturn(leftovers);
        assertFalse(InventoryDelivery.deliver(inventory, purchase));
        verify(inventory).setStorageContents(argThat(slots -> slots.length==1 && slots[0]==null));
    }
    @Test public void deliveryUsesACopyAndSuccessfulInsertionIsNotRolledBack() {
        ItemStack purchase = item(1), copy = item(1);
        when(purchase.clone()).thenReturn(copy);
        PlayerInventory inventory = inventory((ItemStack)null);
        when(inventory.addItem(copy)).thenReturn(new HashMap<>());
        assertTrue(InventoryDelivery.deliver(inventory, purchase));
        verify(inventory).addItem(copy);
        verify(inventory, never()).setStorageContents(any());
    }
}
