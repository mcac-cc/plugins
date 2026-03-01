package com.mcatk.gemshop;

import com.mcatk.gemshop.shops.ShopFactory;
import com.mcatk.gemshop.shops.itemshop.ItemShop;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GemListenerTest {

    @Mock private GemShop gemShop;
    @Mock private ShopFactory shopFactory;
    @Mock private ItemShop itemShop;
    @Mock private Player player;
    @Mock private InventoryClickEvent event;
    @Mock private Inventory topInventory;
    @Mock private Inventory bottomInventory;
    @Mock private InventoryView view;
    @Mock private ItemStack itemStack;
    @Mock private ItemMeta itemMeta;

    private GemListener gemListener;

    @Before
    public void setUp() {
        gemListener = new GemListener();
    }

    @Test
    public void testOnInventoryClick_InPlayerInventory() {
        try (MockedStatic<GemShop> gemShopStatic = Mockito.mockStatic(GemShop.class)) {
            gemShopStatic.when(GemShop::getPlugin).thenReturn(gemShop);

            // Mock event setup
            when(event.getInventory()).thenReturn(topInventory);
            when(event.getView()).thenReturn(view);
            when(view.getTopInventory()).thenReturn(topInventory);

            // Mock Top Inventory (Shop)
            when(topInventory.getTitle()).thenReturn("§6宝石商店-TestShop");

            // Mock Who Clicked (Player)
            when(event.getWhoClicked()).thenReturn(player);

            // IMPORTANT: Simulate click in player inventory (bottom inventory)
            // By returning bottomInventory when getClickedInventory() is called
            when(event.getClickedInventory()).thenReturn(bottomInventory);

            // Execute
            gemListener.onInventoryClick(event);

            // Verify: clicks in player inventory are ignored before any item/lore processing.
            verify(event, never()).getCurrentItem();
            verify(itemShop, never()).buyItem(any(), any(), any());
        }
    }

    @Test
    public void testOnInventoryClick_InShopInventory() {
        try (MockedStatic<GemShop> gemShopStatic = Mockito.mockStatic(GemShop.class)) {
            gemShopStatic.when(GemShop::getPlugin).thenReturn(gemShop);
            when(gemShop.getShopFactory()).thenReturn(shopFactory);
            when(shopFactory.getItemShop()).thenReturn(itemShop);

            // Mock event setup
            when(event.getWhoClicked()).thenReturn(player);
            when(event.getInventory()).thenReturn(topInventory);
            when(event.getView()).thenReturn(view);
            when(view.getTopInventory()).thenReturn(topInventory);

            // Mock Top Inventory (Shop)
            when(topInventory.getTitle()).thenReturn("§6宝石商店-TestShop");

            // Mock Item
            when(event.getCurrentItem()).thenReturn(itemStack);
            when(itemStack.hasItemMeta()).thenReturn(true);
            when(itemStack.getItemMeta()).thenReturn(itemMeta);
            when(itemMeta.getLore()).thenReturn(Collections.singletonList("ID:TestItem"));

            // IMPORTANT: Simulate click in shop inventory (top inventory)
            when(event.getClickedInventory()).thenReturn(topInventory);

            // Execute
            gemListener.onInventoryClick(event);

            // Verify: buyItem SHOULD be called
            verify(itemShop, times(1)).buyItem(player, "TestShop", "TestItem");
        }
    }
}
