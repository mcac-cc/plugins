package com.mcatk.medalcabinet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.mockito.Mockito.*;

class MedalNavigationTest {
    @Test
    void onlyTheBackSlotNavigatesToTheMenu() {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getSize()).thenReturn(54);
        ItemFactory factory = mock(ItemFactory.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(factory.getItemMeta(Material.GOLD_NUGGET)).thenReturn(meta);
        Player player = mock(Player.class);
        PluginManager manager = mock(PluginManager.class);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        when(click.getInventory()).thenReturn(inventory);
        when(click.getWhoClicked()).thenReturn(player);
        ItemStack icon = mock(ItemStack.class);
        when(icon.getType()).thenReturn(Material.STONE);
        when(icon.hasItemMeta()).thenReturn(true);
        when(icon.getItemMeta()).thenReturn(meta);
        when(meta.hasDisplayName()).thenReturn(true);
        when(meta.getDisplayName()).thenReturn("平安返回");
        when(click.getCurrentItem()).thenReturn(icon);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            bukkit.when(() -> Bukkit.createInventory(null, 54, "Alex §6的勋章墙")).thenReturn(inventory);
            MedalsGui gui = new MedalsGui("Alex", Collections.emptyList());
            when(click.getRawSlot()).thenReturn(0);
            gui.onInventoryClick(click);
            verify(player, never()).chat(anyString());
            when(click.getRawSlot()).thenReturn(53);
            gui.onInventoryClick(click);
            verify(player).chat("/menu");
        }
    }
}
