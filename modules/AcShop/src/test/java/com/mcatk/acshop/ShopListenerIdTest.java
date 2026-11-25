package com.mcatk.acshop;

import com.mcatk.acshop.commodity.Item;
import com.mcatk.acshop.commodity.ItemType;
import com.mcatk.acshop.shop.Shop;
import com.mcatk.acshop.shop.Shops;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class ShopListenerIdTest {
    @Test
    public void resolvesTheDisplayedProductAcrossDelimiters() {
        Shops shops = new Shops();
        Shop shop = new Shop("daily-sale");
        Item item = new Item(ItemType.ITEM_STACK, "ore:gold", 5, "templates", "gold");
        shop.getItemHashMap().put(item.getId(), item);
        shops.getShopsHashMap().put(shop.getId(), shop);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        Inventory inventory = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        Player buyer = mock(Player.class);
        ItemStack icon = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(click.getWhoClicked()).thenReturn(buyer);
        when(click.getInventory()).thenReturn(inventory);
        when(click.getClickedInventory()).thenReturn(inventory);
        when(click.getView()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(inventory);
        when(inventory.getTitle()).thenReturn("daily-sale  §6AC商店-daily-sale");
        when(click.getCurrentItem()).thenReturn(icon);
        when(icon.hasItemMeta()).thenReturn(true);
        when(icon.getItemMeta()).thenReturn(meta);
        lenient().when(meta.hasDisplayName()).thenReturn(true);
        lenient().when(meta.getDisplayName()).thenReturn("平安返回");
        when(meta.getLore()).thenReturn(Collections.singletonList("商品ID:ore:gold"));
        try (MockedStatic<AcShop> plugin = mockStatic(AcShop.class);
             MockedConstruction<Operation> purchases = mockConstruction(Operation.class)) {
            plugin.when(AcShop::getShops).thenReturn(shops);
            new ShopListener().onInventoryClick(click);
            verify(purchases.constructed().get(0)).buy(buyer, item);
            when(click.getRawSlot()).thenReturn(53);
            new ShopListener().onInventoryClick(click);
            verify(buyer).chat("/menu_shop");
        }
    }
}
