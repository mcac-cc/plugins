package com.mcatk.gemshop.shops.itemshop;

import com.mcatk.gemshop.GemShop;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ItemShopBenchmarkTest {

    private GemShop mockPlugin;
    private YamlConfiguration mockConfig;
    private Player mockPlayer;
    private PlayerInventory mockInventory;

    @Before
    public void setUp() throws Exception {
        mockPlugin = Mockito.mock(GemShop.class);
        mockConfig = Mockito.mock(YamlConfiguration.class);
        mockPlayer = Mockito.mock(Player.class);
        mockInventory = Mockito.mock(PlayerInventory.class);
        Logger mockLogger = Mockito.mock(Logger.class);

        // Inject mock plugin
        Field pluginField = GemShop.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(null, mockPlugin);

        when(mockPlugin.getConfig()).thenReturn(mockConfig);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        // Fix NPE for new File(dataFolder, ...)
        File tempFolder = new File(System.getProperty("java.io.tmpdir"));
        when(mockPlugin.getDataFolder()).thenReturn(tempFolder);

        when(mockPlayer.getInventory()).thenReturn(mockInventory);
        when(mockInventory.getItemInMainHand()).thenReturn(Mockito.mock(ItemStack.class));

        when(mockConfig.getConfigurationSection(anyString())).thenReturn(null);
        when(mockConfig.saveToString()).thenReturn("key: value");
    }

    @After
    public void tearDown() throws Exception {
        // Reset the static field
        Field pluginField = GemShop.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(null, null);
    }

    @Test
    public void testAddItemPerformance() {
        // Setup slow saveConfig (which should NOT be called now)
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(50); // Simulate 50ms Disk I/O
                return null;
            }
        }).when(mockPlugin).saveConfig();

        ItemShop itemShop = new ItemShop();

        long startTime = System.currentTimeMillis();
        // Price "100" as string
        itemShop.addItem(mockPlayer, "shop1", "item1", "100");
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        System.out.println("Benchmark Execution time: " + duration + "ms");

        // Assert that saveConfig was NOT called (because we use async save)
        verify(mockPlugin, never()).saveConfig();

        // Assert that saveToString WAS called
        verify(mockConfig).saveToString();

        // In optimized code, duration should be near 0ms
        if (duration >= 50) {
             throw new RuntimeException("Execution time too slow: " + duration + "ms. Optimization failed?");
        }
    }

    @Test
    public void testDelItemOptimization() {
        // Setup initial state
        ItemShop itemShop = new ItemShop();
        itemShop.addItem(mockPlayer, "shop1", "item1", "100");

        // Reset mocks to clear interactions from addItem
        Mockito.clearInvocations(mockConfig, mockPlugin);

        itemShop.delItem("shop1", "item1");

        // Assert that saveConfig was NOT called
        verify(mockPlugin, never()).saveConfig();

        // Assert that saveToString WAS called
        verify(mockConfig).saveToString();
    }
}
