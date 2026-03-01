package com.mcatk.gemshop.shops.itemshop;

import com.mcatk.gem.Gem;
import com.mcatk.gemshop.GemShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class ItemShopTest {

    private MockedStatic<GemShop> gemShopStatic;
    private MockedStatic<Gem> gemStatic;
    private GemShop plugin;
    private FileConfiguration config;
    private ItemShop itemShop;

    @Before
    public void setUp() {
        gemShopStatic = Mockito.mockStatic(GemShop.class);
        gemStatic = Mockito.mockStatic(Gem.class);
        plugin = mock(GemShop.class);
        config = mock(FileConfiguration.class);

        gemShopStatic.when(GemShop::getPlugin).thenReturn(plugin);
        when(plugin.getConfig()).thenReturn(config);
        when(config.getConfigurationSection("Items")).thenReturn(null);

        itemShop = new ItemShop();
    }

    @After
    public void tearDown() {
        gemStatic.close();
        gemShopStatic.close();
    }

    @Test
    public void testBuyItemWithMissingShopDoesNotReachGemApi() {
        Player player = mock(Player.class);

        itemShop.buyItem(player, "missingShop", "missingItem");

        verify(player).sendMessage(contains("无该分类"));
        verify(player, never()).getInventory();
        gemStatic.verifyNoInteractions();
    }

    @Test
    public void testDeleteMissingShopDoesNothing() {
        itemShop.delItem("missingShop", "missingItem");

        verify(config, never()).set(anyString(), any());
        verify(plugin, never()).saveConfig();
    }
}
