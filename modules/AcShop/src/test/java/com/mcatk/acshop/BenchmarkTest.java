package com.mcatk.acshop;

import com.mcatk.acshop.commodity.Item;
import com.mcatk.acshop.commodity.ItemType;
import com.mcatk.acshop.shop.Shop;
import com.mcatk.acshop.shop.Shops;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.plugin.Plugin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BenchmarkTest {

    @Test
    public void benchmarkSaveShops() throws IOException {
        // Setup data
        Shops shops = new Shops();
        for (int i = 0; i < 50; i++) {
            Shop shop = new Shop("Shop" + i);
            for (int j = 0; j < 50; j++) {
                shop.getItemHashMap().put("Item" + j, new Item(ItemType.ITEM_STACK, "Item" + j, 100, "sort", "id"));
            }
            shops.getShopsHashMap().put(shop.getId(), shop);
        }

        // Mock AcShop
        File tempDir = Files.createTempDirectory("acshop_test").toFile();
        tempDir.deleteOnExit();

        AcShop mockPlugin = mock(AcShop.class);
        when(mockPlugin.getDataFolder()).thenReturn(tempDir);

        BukkitScheduler mockScheduler = mock(BukkitScheduler.class);
        when(mockScheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenReturn(null);

        try (MockedStatic<AcShop> mockedAcShop = Mockito.mockStatic(AcShop.class);
             MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {

            mockedAcShop.when(AcShop::getPlugin).thenReturn(mockPlugin);
            mockedBukkit.when(Bukkit::getScheduler).thenReturn(mockScheduler);

            // Warmup
            new FileOperation().saveShopsAsync(shops);

            // Benchmark
            long startTime = System.nanoTime();
            int iterations = 20;
            for (int i = 0; i < iterations; i++) {
                new FileOperation().saveShopsAsync(shops);
            }
            long endTime = System.nanoTime();
            double avgTime = (endTime - startTime) / (double) iterations / 1_000_000.0;
            System.out.println("Average save time (Async offload): " + avgTime + " ms");
        }
    }
}
