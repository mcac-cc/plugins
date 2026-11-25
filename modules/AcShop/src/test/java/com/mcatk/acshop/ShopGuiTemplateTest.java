package com.mcatk.acshop;

import com.mcatk.acshop.commodity.Item;
import com.mcatk.acshop.commodity.ItemType;
import com.mcatk.acshop.shop.Shop;
import com.mcatk.itemmanager.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

public class ShopGuiTemplateTest {
    @Test
    public void missingTemplateDoesNotHideTheNextProduct() {
        Shop shop = new Shop("tools");
        for (String id : new String[]{"absent", "stone"}) {
            shop.getItemHashMap().put(id, new Item(ItemType.ITEM_STACK, id, 5, "tools", id));
        }
        Inventory gui = mock(Inventory.class);
        ItemFactory factory = mock(ItemFactory.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(factory.getItemMeta(any(Material.class))).thenReturn(meta);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ItemManager> templates = mockStatic(ItemManager.class)) {
            bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
            bukkit.when(() -> Bukkit.createInventory(null, 54, "tools  §6AC商店-tools")).thenReturn(gui);
            templates.when(() -> ItemManager.getItem("tools", "stone")).thenReturn(new ItemStack(Material.STONE));
            new ShopGui(shop).getGui();
            verify(gui).setItem(eq(0), argThat(icon -> icon.getType() == Material.STONE));
            verify(gui).setItem(eq(53), argThat(icon -> icon.getType() == Material.GOLD_NUGGET));
        }
    }
}
