package com.mcatk.gemshop.commands;

import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.shops.ShopFactory;
import com.mcatk.gemshop.shops.itemshop.ItemShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class CommandGsiTest {

    private MockedStatic<GemShop> gemShopStatic;
    private final CommandGsi commandGsi = new CommandGsi();

    @Before
    public void setUp() {
        gemShopStatic = Mockito.mockStatic(GemShop.class);
    }

    @After
    public void tearDown() {
        gemShopStatic.close();
    }

    @Test
    public void testNonOpSenderRejected() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.isOp()).thenReturn(false);

        boolean result = commandGsi.onCommand(sender, mock(Command.class), "gsi", new String[]{"list"});

        assertFalse(result);
    }

    @Test
    public void testAddRequiresPlayerSender() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.isOp()).thenReturn(true);

        boolean result = commandGsi.onCommand(
                sender, mock(Command.class), "gsi", new String[]{"add", "shop", "item", "10"}
        );

        assertTrue(result);
        verify(sender).sendMessage(contains("仅玩家可执行"));
    }

    @Test
    public void testAddWithMissingArgsShowsHelp() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.isOp()).thenReturn(true);

        boolean result = commandGsi.onCommand(sender, mock(Command.class), "gsi", new String[]{"add"});

        assertTrue(result);
        verify(sender, times(3)).sendMessage(anyString());
    }

    @Test
    public void testAddCallsItemShop() {
        Player sender = mock(Player.class);
        when(sender.isOp()).thenReturn(true);
        GemShop plugin = mock(GemShop.class);
        ShopFactory shopFactory = mock(ShopFactory.class);
        ItemShop itemShop = mock(ItemShop.class);

        gemShopStatic.when(GemShop::getPlugin).thenReturn(plugin);
        when(plugin.getShopFactory()).thenReturn(shopFactory);
        when(shopFactory.getItemShop()).thenReturn(itemShop);

        boolean result = commandGsi.onCommand(
                sender, mock(Command.class), "gsi", new String[]{"add", "shop", "item", "10"}
        );

        assertTrue(result);
        verify(itemShop).addItem(sender, "shop", "item", "10");
    }

    @Test
    public void testAddRejectsNonNumericPrice() {
        Player sender = mock(Player.class);
        when(sender.isOp()).thenReturn(true);

        boolean result = commandGsi.onCommand(
                sender, mock(Command.class), "gsi", new String[]{"add", "shop", "item", "abc"}
        );

        assertTrue(result);
        verify(sender).sendMessage(contains("价格必须为正整数"));
        gemShopStatic.verifyNoInteractions();
    }

    @Test
    public void testAddRejectsNonPositivePrice() {
        Player sender = mock(Player.class);
        when(sender.isOp()).thenReturn(true);

        boolean result = commandGsi.onCommand(
                sender, mock(Command.class), "gsi", new String[]{"add", "shop", "item", "0"}
        );

        assertTrue(result);
        verify(sender).sendMessage(contains("价格必须为正整数"));
        gemShopStatic.verifyNoInteractions();
    }
}
