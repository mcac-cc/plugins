package com.mcatk.gemshop.shops.vipshop;

import com.mcatk.gem.Gem;
import com.mcatk.gem.GemExecutor;
import com.mcatk.gemshop.ServerCmd;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class VipShopTest {

    @Test
    public void testBuyVipVulnerability() {
        try (MockedStatic<Gem> mockedGemStatic = mockStatic(Gem.class);
             MockedStatic<ServerCmd> mockedServerCmd = mockStatic(ServerCmd.class);
             MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {

            // Mock Bukkit logger
            Logger mockLogger = mock(Logger.class);
            mockedBukkit.when(Bukkit::getLogger).thenReturn(mockLogger);

            // Mock Gem plugin and executor
            Gem mockGem = mock(Gem.class);
            GemExecutor mockExecutor = mock(GemExecutor.class);

            mockedGemStatic.when(Gem::getPlugin).thenReturn(mockGem);
            when(mockGem.getGemExecutor()).thenReturn(mockExecutor);
            // Assume player has enough gems
            when(mockExecutor.takeGems(anyString(), anyInt())).thenReturn(true);

            // Mock Player
            Player mockPlayer = mock(Player.class);
            when(mockPlayer.getName()).thenReturn("TestPlayer");
            when(mockPlayer.hasPermission(anyString())).thenReturn(false);

            // Test execution
            VipShop vipShop = new VipShop();
            // "admin" is not a valid vip type, so it should be rejected
            vipShop.buyVip(mockPlayer, "admin");

            // Verify that ServerCmd.sendConsoleCmd was NOT called
            mockedServerCmd.verify(() -> ServerCmd.sendConsoleCmd(anyString()), never());

            // Also verify that player received an error message (optional)
            // verify(mockPlayer).sendMessage(contains("参数错误"));
        }
    }

    @Test
    public void testBuyVipSuccess() {
        try (MockedStatic<Gem> mockedGemStatic = mockStatic(Gem.class);
             MockedStatic<ServerCmd> mockedServerCmd = mockStatic(ServerCmd.class);
             MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {

            // Mock Bukkit logger
            Logger mockLogger = mock(Logger.class);
            mockedBukkit.when(Bukkit::getLogger).thenReturn(mockLogger);

            // Mock Gem plugin and executor
            Gem mockGem = mock(Gem.class);
            GemExecutor mockExecutor = mock(GemExecutor.class);

            mockedGemStatic.when(Gem::getPlugin).thenReturn(mockGem);
            when(mockGem.getGemExecutor()).thenReturn(mockExecutor);
            // Assume player has enough gems
            when(mockExecutor.takeGems(anyString(), anyInt())).thenReturn(true);

            // Mock Player
            Player mockPlayer = mock(Player.class);
            when(mockPlayer.getName()).thenReturn("TestPlayer");
            when(mockPlayer.hasPermission(anyString())).thenReturn(false);

            // Test execution with valid type
            VipShop vipShop = new VipShop();
            vipShop.buyVip(mockPlayer, "vip");

            // Verify that ServerCmd.sendConsoleCmd was called correctly
            mockedServerCmd.verify(() -> ServerCmd.sendConsoleCmd(contains("vip 30d")), times(1));
        }
    }
}
