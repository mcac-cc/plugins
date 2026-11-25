package com.mcatk.gemshop.shops.itemshop;

import com.mcatk.gem.Gem;
import com.mcatk.gemshop.GemShop;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.junit.Assert;
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

    @Test
    public void emptyHandDoesNotReplaceAnExistingListing() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.STONE))
                .thenReturn(new ItemStack(Material.AIR));
        itemShop.addItem(player, "tools", "stone", "3");
        Item original = itemShop.getItems("tools").getMap().get("stone");
        itemShop.addItem(player, "tools", "stone", "4");
        Assert.assertSame(original, itemShop.getItems("tools").getMap().get("stone"));
        verify(plugin, times(1)).saveConfigAsync();
        verify(player).sendMessage(contains("手持"));
    }

    @Test
    public void loadsValidItemsAlongsideAnOldEmptyListing() {
        YamlConfiguration saved = new YamlConfiguration();
        Item valid = new Item("stone", new ItemStack(Material.STONE), 3);
        saved.set("Items.tools.stone", valid);
        saved.set("Items.tools.empty", new Item("empty", new ItemStack(Material.AIR), 4));
        when(plugin.getConfig()).thenReturn(saved);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        Items loaded = new ItemShop().getItems("tools");
        Assert.assertSame(valid, loaded.getMap().get("stone"));
        Assert.assertFalse(loaded.getMap().containsKey("empty"));
    }
}
