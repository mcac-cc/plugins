package com.mcatk.gem.command;

import com.mcatk.gem.Gem;
import com.mcatk.gem.GemExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommandGemBenchmarkTest {

    private Gem gemMock;
    private GemExecutor gemExecutorMock;
    private CommandSender senderMock;
    private Command commandMock;
    private Server serverMock;
    private BukkitScheduler schedulerMock;

    @Before
    public void setUp() throws Exception {
        // Mock Gem.plugin (static field)
        gemMock = mock(Gem.class);
        Field pluginField = Gem.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(null, gemMock);

        // Mock GemExecutor
        gemExecutorMock = mock(GemExecutor.class);
        lenient().when(gemMock.getGemExecutor()).thenReturn(gemExecutorMock);
        lenient().when(gemMock.getLogger()).thenReturn(Logger.getLogger("GemTest"));

        // Mock Bukkit.server (static)
        serverMock = mock(Server.class);
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, serverMock);

        // Mock Scheduler
        schedulerMock = mock(BukkitScheduler.class);
        lenient().when(serverMock.getScheduler()).thenReturn(schedulerMock);

        // Mock generic runTaskAsynchronously
        lenient().when(schedulerMock.runTaskAsynchronously(any(Plugin.class), any(Runnable.class)))
            .thenReturn(mock(BukkitTask.class));

        // Mock CommandSender
        senderMock = mock(CommandSender.class);
        when(senderMock.isOp()).thenReturn(true);

        // Mock Command
        commandMock = mock(Command.class);
    }

    @Test
    public void testSetGemsAsync() {
        CommandGem commandGem = new CommandGem();
        String[] args = new String[]{"set", "player", "100"};

        long startTime = System.currentTimeMillis();
        commandGem.onCommand(senderMock, commandMock, "gem", args);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        System.out.println("Execution time: " + duration + "ms");

        // Assert that it returns immediately (non-blocking)
        assertTrue("Execution should be async and take < 50ms, but took " + duration + "ms", duration < 50);

        // Capture and run the task
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(schedulerMock).runTaskAsynchronously(any(Plugin.class), runnableCaptor.capture());

        runnableCaptor.getValue().run();

        // Verify logic executed
        verify(gemExecutorMock).setGems("player", 100);
        verify(senderMock).sendMessage(contains("100"));
    }
}
