package com.mcatk.itemmanager;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ItemManagerCommandTest {
    @Test
    public void emptyCategorySurvivesReloadAndRepeatedCreatePreservesItems() throws Exception {
        ItemManager plugin = mock(ItemManager.class);
        YamlConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        CommandSender sender = mock(CommandSender.class);
        when(sender.isOp()).thenReturn(true);
        try (MockedStatic<ItemManager> manager = mockStatic(ItemManager.class)) {
            manager.when(ItemManager::getPlugin).thenReturn(plugin);
            ItemSort original = new ItemSort();
            manager.when(ItemManager::getItemSort).thenReturn(original);
            ItemManagerCommand command = new ItemManagerCommand();
            command.onCommand(sender, null, "im", new String[]{"create", "tools"});
            verify(plugin).saveConfig();

            YamlConfiguration restored = new YamlConfiguration();
            restored.loadFromString(config.saveToString());
            when(plugin.getConfig()).thenReturn(restored);
            ItemSort reloaded = new ItemSort();
            assertTrue(reloaded.listAll().contains("类型(tools)"));
            manager.when(ItemManager::getItemSort).thenReturn(reloaded);
            ItemStack stone = new ItemStack(Material.STONE);
            reloaded.addItem("tools", "stone", stone);
            command.onCommand(sender, null, "im", new String[]{"create", "tools"});
            assertEquals(stone, reloaded.getItem("tools", "stone"));
            assertEquals(stone, restored.get("tools.stone"));
        }
    }

    @Test
    public void getReportsTheUndeliveredAmount() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.isOp()).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack requested = new ItemStack(Material.STONE, 8);
        ItemSort sort = mock(ItemSort.class);
        when(sort.getItem("tools", "stone")).thenReturn(requested);
        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, new ItemStack(Material.STONE, 3));
        HashMap<Integer, ItemStack> full = new HashMap<>();
        full.put(0, requested.clone());
        when(inventory.addItem(any(ItemStack.class))).thenReturn(leftovers).thenReturn(full)
                .thenReturn(new HashMap<>());
        try (MockedStatic<ItemManager> manager = mockStatic(ItemManager.class)) {
            manager.when(ItemManager::getItemSort).thenReturn(sort);
            ItemManagerCommand command = new ItemManagerCommand();
            String[] args = {"get", "tools", "stone"};
            command.onCommand(player, null, "im", args);
            verify(player).sendMessage("背包空间不足，还有 3 个物品未领取。");
            command.onCommand(player, null, "im", args);
            verify(player).sendMessage("背包空间不足，还有 8 个物品未领取。");
            verify(player, never()).sendMessage("Ok");
            command.onCommand(player, null, "im", args);
            verify(player).sendMessage("Ok");
        }
    }
}
