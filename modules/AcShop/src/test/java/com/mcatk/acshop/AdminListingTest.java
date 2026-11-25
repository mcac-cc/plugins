package com.mcatk.acshop;

import com.mcatk.acshop.command.AdminCommand;
import com.mcatk.acshop.shop.Shops;
import com.mcatk.itemmanager.ItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AdminListingTest {
    @Test
    public void rejectedListingsLeaveBothCatalogsUnchanged() {
        Shops shops = new Shops();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.isOp()).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.AIR));
        try (MockedStatic<AcShop> plugin = mockStatic(AcShop.class);
             MockedStatic<ItemManager> templates = mockStatic(ItemManager.class);
             MockedConstruction<FileOperation> files = mockConstruction(FileOperation.class)) {
            plugin.when(AcShop::getShops).thenReturn(shops);
            AdminCommand command = new AdminCommand();
            command.onCommand(player, null, "asadmin", new String[]{"add", "item", "badprice", "egg", "0"});
            command.onCommand(player, null, "asadmin", new String[]{"add", "item", "empty", "egg", "5"});
            when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.STONE));
            command.onCommand(player, null, "asadmin", new String[]{"add", "item", "tools", "ore.gold", "5"});
            command.onCommand(player, null, "asadmin", new String[]{"add", "item", "tools.ore", "gold", "5"});
            assertTrue(shops.getShopsHashMap().isEmpty());
            templates.verifyNoInteractions();
            assertTrue(files.constructed().isEmpty());
        }
    }
}
