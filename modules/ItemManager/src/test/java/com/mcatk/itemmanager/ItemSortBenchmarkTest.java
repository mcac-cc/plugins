package com.mcatk.itemmanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ItemSortBenchmarkTest {

    private ItemManager itemManager;
    private YamlConfiguration config;
    private ItemSort itemSort;

    @Before
    public void setUp() throws Exception {
        itemManager = mock(ItemManager.class);
        config = mock(YamlConfiguration.class);
        Logger logger = mock(Logger.class);

        // Set static plugin field
        Field pluginField = ItemManager.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(null, itemManager);

        when(itemManager.getConfig()).thenReturn(config);
        when(itemManager.getLogger()).thenReturn(logger);
        when(config.getKeys(false)).thenReturn(new HashSet<>());

        // Mock saveConfig to simulate I/O delay
        doAnswer((Answer<Void>) invocation -> {
            try {
                Thread.sleep(10); // Simulate 10ms disk I/O
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }).when(itemManager).saveConfig();

        itemSort = new ItemSort();
    }

    @Test
    public void benchmarkAddItem() {
        long startTime = System.currentTimeMillis();
        int iterations = 50;

        for (int i = 0; i < iterations; i++) {
            itemSort.addItem("sort1", "item" + i, mock(ItemStack.class));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Time taken for " + iterations + " adds: " + duration + "ms");

        // Now it should be fast (< 100ms) because saveConfigAsync (mocked) does nothing
        assertTrue("Should be fast (< 100ms) with async saving", duration < 100);

        // Verify saveConfigAsync was called 50 times
        verify(itemManager, times(iterations)).saveConfigAsync();

        // Verify synchronous saveConfig was NOT called (except maybe in constructor? No, constructor calls config.getKeys)
        // ItemSort constructor calls getConfig().getKeys(), not saveConfig().
        // So saveConfig() should be called 0 times.
        verify(itemManager, times(0)).saveConfig();
    }
}
