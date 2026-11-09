package com.mcatk.acshop;

import com.mcatk.acshop.commodity.Item;
import com.mcatk.acshop.commodity.ItemType;
import com.mcatk.itemmanager.ItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.MockedStatic;
import java.util.HashMap;
import java.util.logging.Logger;
import static org.mockito.Mockito.*;

public class OperationTest {
    @Test public void fullBagCannotBeCharged() {
        Player player=mock(Player.class);PlayerInventory inventory=mock(PlayerInventory.class);
        ItemStack purchase=mock(ItemStack.class),full=mock(ItemStack.class);
        when(purchase.clone()).thenReturn(purchase);when(purchase.getType()).thenReturn(Material.STONE);
        when(purchase.getAmount()).thenReturn(1);when(purchase.getMaxStackSize()).thenReturn(64);
        when(full.getType()).thenReturn(Material.DIRT);when(inventory.getStorageContents()).thenReturn(new ItemStack[]{full});
        when(inventory.getMaxStackSize()).thenReturn(64);when(player.getInventory()).thenReturn(inventory);
        try(MockedStatic<ItemManager> items=mockStatic(ItemManager.class);MockedStatic<VaultApi> vault=mockStatic(VaultApi.class)) {
            items.when(()->ItemManager.getItem("sort","item")).thenReturn(purchase);
            new Operation().buy(player,new Item(ItemType.ITEM_STACK,"id",100,"sort","item"));
            vault.verify(()->VaultApi.takePlayerMoney(any(),anyDouble()),never());
            verify(player,never()).sendMessage("购买成功");
        }
    }
    @Test public void failedInsertionRollsBackAndRefundsWithoutSuccessMessage() {
        Player player=mock(Player.class);PlayerInventory inventory=mock(PlayerInventory.class);
        ItemStack purchase=mock(ItemStack.class);
        when(purchase.clone()).thenReturn(purchase);when(purchase.getType()).thenReturn(Material.STONE);
        when(purchase.getAmount()).thenReturn(1);when(purchase.getMaxStackSize()).thenReturn(64);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{null});when(inventory.getMaxStackSize()).thenReturn(64);
        when(player.getInventory()).thenReturn(inventory);
        HashMap<Integer,ItemStack> leftovers=new HashMap<>();leftovers.put(0,purchase);
        when(inventory.addItem(purchase)).thenReturn(leftovers);
        try(MockedStatic<ItemManager> items=mockStatic(ItemManager.class);MockedStatic<VaultApi> vault=mockStatic(VaultApi.class)) {
            items.when(()->ItemManager.getItem("sort","item")).thenReturn(purchase);
            vault.when(()->VaultApi.takePlayerMoney(player,100)).thenReturn(true);
            vault.when(()->VaultApi.refundPlayerMoney(player,100)).thenReturn(true);
            new Operation().buy(player,new Item(ItemType.ITEM_STACK,"id",100,"sort","item"));
            verify(inventory).setStorageContents(any());
            vault.verify(()->VaultApi.refundPlayerMoney(player,100));verify(player,never()).sendMessage("购买成功");
        }
    }
    @Test public void invalidItemAndEmptyCommandAreRejectedBeforePayment() {
        Player player=mock(Player.class);
        try(MockedStatic<ItemManager> items=mockStatic(ItemManager.class);MockedStatic<VaultApi> vault=mockStatic(VaultApi.class)) {
            new Operation().buy(player,new Item(ItemType.ITEM_STACK,"missing",100,"sort","item"));
            new Operation().buy(player,new Item(ItemType.ITEM_CMD,"empty",100,""));
            vault.verify(()->VaultApi.takePlayerMoney(any(),anyDouble()),never());
        }
    }
}
