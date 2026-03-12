package com.mcatk.gemshop.commands;

import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.shops.ShopFactory;
import com.mcatk.gemshop.shops.itemshop.ItemShop;
import com.mcatk.gemshop.shops.itemshop.Items;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class CommandGemShopTest {

    private MockedStatic<GemShop> gemShopStatic;
    private final CommandGemShop commandGemShop = new CommandGemShop();

    @Before
    public void setUp() {
        gemShopStatic = Mockito.mockStatic(GemShop.class);
    }

    @After
    public void tearDown() {
        gemShopStatic.close();
    }

    @Test
    public void testItemShopRejectsMissingCategory() {
        Player sender = mock(Player.class);
        GemShop plugin = mock(GemShop.class);
        ShopFactory shopFactory = mock(ShopFactory.class);
        ItemShop itemShop = mock(ItemShop.class);

        gemShopStatic.when(GemShop::getPlugin).thenReturn(plugin);
        when(plugin.getShopFactory()).thenReturn(shopFactory);
        when(shopFactory.getItemShop()).thenReturn(itemShop);
        when(itemShop.getItems("missing")).thenReturn(null);

        boolean result = commandGemShop.onCommand(
                sender,
                mock(Command.class),
                "gemshop",
                new String[]{"item", "missing"}
        );

        assertTrue(result);
        verify(sender).sendMessage(contains("无该分类"));
        verify(sender, never()).openInventory(any());
    }

    @Test
    public void testItemShopOpensExistingCategory() {
        Player sender = mock(Player.class);
        GemShop plugin = mock(GemShop.class);
        ShopFactory shopFactory = mock(ShopFactory.class);
        ItemShop itemShop = mock(ItemShop.class);
        Items items = mock(Items.class);

        gemShopStatic.when(GemShop::getPlugin).thenReturn(plugin);
        when(plugin.getShopFactory()).thenReturn(shopFactory);
        when(shopFactory.getItemShop()).thenReturn(itemShop);
        when(itemShop.getItems("weapons")).thenReturn(items);
        when(items.getId()).thenReturn("weapons");
        when(items.getMap()).thenReturn(new java.util.HashMap<>());

        boolean result = commandGemShop.onCommand(
                sender,
                mock(Command.class),
                "gemshop",
                new String[]{"item", "weapons"}
        );

        assertTrue(result);
        verify(sender).openInventory(any());
    }
}
