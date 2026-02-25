package com.mcatk.gemshop.shops.itemshop;

import com.mcatk.gem.Gem;
import com.mcatk.gem.GemExecutor;
import com.mcatk.gemshop.GemShop;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ItemShopTest {

    @Mock private GemShop gemShop;
    @Mock private Gem gem;
    @Mock private GemExecutor gemExecutor;
    @Mock private FileConfiguration config;
    @Mock private ConfigurationSection itemsSection;
    @Mock private Player player;
    @Mock private PlayerInventory inventory;
    @Mock private BukkitScheduler scheduler;
    @Mock private Server server;
    @Mock private Logger logger;

    private MockedStatic<GemShop> gemShopStatic;
    private MockedStatic<Gem> gemStatic;
    private MockedStatic<Bukkit> bukkitStatic;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock static classes
        gemShopStatic = mockStatic(GemShop.class);
        gemStatic = mockStatic(Gem.class);
        bukkitStatic = mockStatic(Bukkit.class);

        // Setup static mocks
        gemShopStatic.when(GemShop::getPlugin).thenReturn(gemShop);
        gemStatic.when(Gem::getPlugin).thenReturn(gem);
        bukkitStatic.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkitStatic.when(Bukkit::getLogger).thenReturn(logger);
        bukkitStatic.when(Bukkit::getServer).thenReturn(server);

        // Setup basic behavior
        when(gemShop.getConfig()).thenReturn(config);
        when(gem.getGemExecutor()).thenReturn(gemExecutor);
        when(server.getLogger()).thenReturn(logger);

        // Mock scheduler to run tasks immediately or capture them
        // For async tasks, we just want to verify they are scheduled
        when(scheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run(); // Run it immediately for testing logic inside, but verification is key
            return null;
        });

        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        });

        // Setup player
        when(player.getInventory()).thenReturn(inventory);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOnline()).thenReturn(true);

        // Setup ItemShop config
        when(config.getConfigurationSection("Items")).thenReturn(itemsSection);
        when(itemsSection.getKeys(false)).thenReturn(new HashSet<>());
    }

    @After
    public void tearDown() {
        gemShopStatic.close();
        gemStatic.close();
        bukkitStatic.close();
    }

    @Test
    public void testBuyItem_Async() {
        // Setup items
        ItemShop itemShop = new ItemShop();

        // We need to inject items into the map because constructor loads from config
        // which we mocked as empty. Or we can mock the config to return items.
        // Let's use reflection or add a method to inject, but ItemShop has getItems()
        // Wait, itemsMap is private.
        // But we can use addItem to add an item if we mock the config set/save

        // Re-mock config for addItem
        when(config.getConfigurationSection(anyString())).thenReturn(itemsSection);

        // Mock item in hand for addItem
        ItemStack itemStack = mock(ItemStack.class);
        when(inventory.getItemInMainHand()).thenReturn(itemStack);

        // Add item to shop
        itemShop.addItem(player, "shop1", "item1", "100");

        // Reset invocations to clear the addItem calls
        clearInvocations(scheduler, gemExecutor);

        // Test buyItem
        // We expect takeGems to be called.
        // Since we mocked runTaskAsynchronously to run immediately,
        // takeGems should be called within the test method execution.

        when(gemExecutor.takeGems("TestPlayer", 100)).thenReturn(true);

        itemShop.buyItem(player, "shop1", "item1");

        // Verify takeGems was called
        verify(gemExecutor).takeGems("TestPlayer", 100);

        // Verify it was run asynchronously
        // Since we mocked runTaskAsynchronously, we verify it was called.
        // Note: verify(scheduler) might be tricky if addItem also used it?
        // addItem uses saveConfig which might be async? No, saveConfig in GemShop is likely sync unless optimized.
        // Memory says: "ItemManager module employs a single-threaded ExecutorService... GemShop module optimizes configuration saving..."

        verify(scheduler, atLeastOnce()).runTaskAsynchronously(eq(gemShop), any(Runnable.class));
    }
}
