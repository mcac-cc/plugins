package com.mcatk.medalcabinet;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import java.util.*;
import static org.mockito.Mockito.*;

class MedalsGuiTest {
    @Test void closesItsListenerAndRejectsDraggingIntoTheMenu() {
        Inventory inventory=mock(Inventory.class);when(inventory.getSize()).thenReturn(54);
        PluginManager plugins=mock(PluginManager.class);MedalCabinet plugin=mock(MedalCabinet.class);
        ItemMeta meta=mock(ItemMeta.class);
        try(MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class);
            MockedStatic<MedalCabinet> global=mockStatic(MedalCabinet.class);
            MockedStatic<HandlerList> handlers=mockStatic(HandlerList.class);
            MockedConstruction<ItemStack> stacks=mockConstruction(ItemStack.class,(item,context)->when(item.getItemMeta()).thenReturn(meta))) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(()->Bukkit.createInventory(null,54,"Alice §6的勋章墙")).thenReturn(inventory);
            global.when(MedalCabinet::getPlugin).thenReturn(plugin);
            MedalsGui gui=new MedalsGui("Alice",Collections.emptyList());
            verify(plugins).registerEvents(gui,plugin);
            InventoryDragEvent drag=mock(InventoryDragEvent.class);when(drag.getInventory()).thenReturn(inventory);
            when(drag.getRawSlots()).thenReturn(new HashSet<>(Arrays.asList(0,55)));
            gui.onInventoryDrag(drag);verify(drag).setCancelled(true);
            InventoryCloseEvent close=mock(InventoryCloseEvent.class);when(close.getInventory()).thenReturn(inventory);
            gui.onInventoryClose(close);handlers.verify(()->HandlerList.unregisterAll(gui));
        }
    }
}
