package com.mcatk.gemshop.shops.vipshop;

import com.mcatk.gem.Gem;
import com.mcatk.gem.GemExecutor;
import com.mcatk.gemshop.ServerCmd;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class VipShopTest {

    private MockedStatic<Gem> gemMockedStatic;
    private MockedStatic<ServerCmd> serverCmdMockedStatic;
    private MockedStatic<Bukkit> bukkitMockedStatic;
    private Gem gemPlugin;
    private GemExecutor gemExecutor;
    private Player player;
    private Logger logger;

    @Before
    public void setUp() {
        gemMockedStatic = Mockito.mockStatic(Gem.class);
        serverCmdMockedStatic = Mockito.mockStatic(ServerCmd.class);
        bukkitMockedStatic = Mockito.mockStatic(Bukkit.class);

        gemPlugin = mock(Gem.class);
        gemExecutor = mock(GemExecutor.class);
        player = mock(Player.class);
        logger = mock(Logger.class);

        gemMockedStatic.when(Gem::getPlugin).thenReturn(gemPlugin);
        when(gemPlugin.getGemExecutor()).thenReturn(gemExecutor);
        when(player.getName()).thenReturn("TestPlayer");
        when(gemExecutor.takeGems(anyString(), anyInt())).thenReturn(true);

        bukkitMockedStatic.when(Bukkit::getLogger).thenReturn(logger);
    }

    @After
    public void tearDown() {
        gemMockedStatic.close();
        serverCmdMockedStatic.close();
        bukkitMockedStatic.close();
    }

    @Test
    public void testBuyVipWithMaliciousInput() {
        VipShop vipShop = new VipShop();
        String maliciousInput = "vip; op hacker"; // Attempt command injection

        // This should now be rejected
        vipShop.buyVip(player, maliciousInput);

        // Verify that sendConsoleCmd was NEVER called
        serverCmdMockedStatic.verify(() -> ServerCmd.sendConsoleCmd(anyString()), never());

        // Verify that the player received an error message (optional, but good practice)
        verify(player).sendMessage(contains("无效的VIP类型"));
    }

    @Test
    public void testBuyVipWithValidInput() {
        VipShop vipShop = new VipShop();
        String validInput = "vip";

        // This should succeed
        vipShop.buyVip(player, validInput);

        // Verify that sendConsoleCmd WAS called with the correct command
        serverCmdMockedStatic.verify(() -> ServerCmd.sendConsoleCmd(
            eq("lp user TestPlayer parent addtemp vip 30d")
        ));
    }
}
