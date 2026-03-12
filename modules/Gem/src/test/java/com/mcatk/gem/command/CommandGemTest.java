package com.mcatk.gem.command;

import com.mcatk.gem.Gem;
import com.mcatk.gem.GemExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CommandGemTest {

    private final CommandGem commandGem = new CommandGem();
    private MockedStatic<Gem> gemStatic;
    private MockedStatic<Bukkit> bukkitStatic;
    private Gem plugin;
    private GemExecutor gemExecutor;

    @Before
    public void setUp() {
        gemStatic = Mockito.mockStatic(Gem.class);
        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        plugin = mock(Gem.class);
        gemExecutor = mock(GemExecutor.class);

        gemStatic.when(Gem::getPlugin).thenReturn(plugin);
        when(plugin.getGemExecutor()).thenReturn(gemExecutor);
    }

    @After
    public void tearDown() {
        bukkitStatic.close();
        gemStatic.close();
    }

    @Test
    public void testAddRejectsNonPositiveAmount() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.isOp()).thenReturn(true);

        boolean result = commandGem.onCommand(sender, mock(Command.class), "gem", new String[]{"add", "Steve", "0"});

        assertTrue(result);
        verify(sender).sendMessage(contains("宝石数量必须大于 0"));
        verifyNoInteractions(gemExecutor);
    }

    @Test
    public void testTakeRejectsNegativeAmount() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.isOp()).thenReturn(true);

        boolean result = commandGem.onCommand(sender, mock(Command.class), "gem", new String[]{"take", "Steve", "-5"});

        assertTrue(result);
        verify(sender).sendMessage(contains("宝石数量必须大于 0"));
        verifyNoInteractions(gemExecutor);
    }

    @Test
    public void testSetRejectsNegativeAmount() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.isOp()).thenReturn(true);

        boolean result = commandGem.onCommand(sender, mock(Command.class), "gem", new String[]{"set", "Steve", "-1"});

        assertTrue(result);
        verify(sender).sendMessage(contains("宝石数量必须大于 0"));
        verifyNoInteractions(gemExecutor);
    }
}
