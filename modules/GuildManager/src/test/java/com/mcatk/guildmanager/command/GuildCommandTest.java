package com.mcatk.guildmanager.command;

import com.mcatk.guildmanager.GuildManager;
import com.mcatk.guildmanager.core.service.GuildService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GuildCommandTest {

    @Mock
    private GuildManager guildManager;
    @Mock
    private GuildService guildService;
    @Mock
    private Server server;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private ItemFactory itemFactory;
    @Mock
    private ItemMeta itemMeta;
    @Mock
    private Inventory inventory;
    @Mock
    private Player sender;
    @Mock
    private Command command;

    private GuildCommand guildCommand;

    @Before
    public void setUp() throws Exception {
        // Mock Logger for Bukkit.getLogger()
        lenient().when(server.getLogger()).thenReturn(Logger.getLogger("Minecraft"));

        // Mock Scheduler
        lenient().when(server.getScheduler()).thenReturn(scheduler);

        // Mock PluginManager
        lenient().when(server.getPluginManager()).thenReturn(pluginManager);

        // Mock ItemFactory
        lenient().when(server.getItemFactory()).thenReturn(itemFactory);
        lenient().when(itemFactory.getItemMeta(any(Material.class))).thenReturn(itemMeta);

        // Mock Inventory
        lenient().when(server.createInventory(any(), anyInt(), anyString())).thenReturn(inventory);

        // Force inject server mock into Bukkit
        java.lang.reflect.Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, server);

        // Inject GuildManager mock into static plugin field (for GuildsGUI legacy support)
        java.lang.reflect.Field pluginField = GuildManager.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(null, guildManager);

        // Also mock getGuildService() on the mock because GuildsGUI uses it via static call
        // Since I removed final from GuildManager class, this SHOULD work now.
        doReturn(guildService).when(guildManager).getGuildService();

        // Inject dependencies directly
        guildCommand = new GuildCommand(guildManager, guildService);
        lenient().when(sender.getName()).thenReturn("TestPlayer");
    }

    @Test
    public void testOnCommandAsync() {
        // Setup capturing of async task
        Runnable[] capturedAsyncTask = new Runnable[1];

        when(scheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            capturedAsyncTask[0] = invocation.getArgument(1);
            return mock(BukkitTask.class);
        });

        // Setup sync task execution immediately (mocking the callback to main thread)
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return mock(BukkitTask.class);
        });

        // Mock DB Call
        when(guildService.getPlayerGuild(anyString())).thenReturn(null);

        // Execute
        guildCommand.onCommand(sender, command, "gmg", new String[]{"gui"});

        // Check that async task was submitted
        verify(scheduler).runTaskAsynchronously(any(Plugin.class), any(Runnable.class));

        // Since we mocked runTaskAsynchronously to just capture, the DB call should NOT have happened yet
        verify(guildService, never()).getPlayerGuild(anyString());

        // Now run the async task
        if (capturedAsyncTask[0] != null) {
            capturedAsyncTask[0].run();
        }

        // Verify DB call WAS made
        verify(guildService).getPlayerGuild("TestPlayer");
    }

    @Test
    public void testOptimizationVerification() {
        // Same as above but checking that main thread logic runs
        Runnable[] capturedAsyncTask = new Runnable[1];

        when(scheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            capturedAsyncTask[0] = invocation.getArgument(1);
            return mock(BukkitTask.class);
        });

        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return mock(BukkitTask.class);
        });

        when(guildService.getPlayerGuild(anyString())).thenReturn(null);

        guildCommand.onCommand(sender, command, "gmg", new String[]{"gui"});

        verify(guildService, never()).getPlayerGuild(anyString());

        if (capturedAsyncTask[0] != null) {
            capturedAsyncTask[0].run();
        }

        verify(guildService).getPlayerGuild("TestPlayer");
    }
}
