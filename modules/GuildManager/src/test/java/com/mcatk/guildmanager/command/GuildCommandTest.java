package com.mcatk.guildmanager.command;

import com.mcatk.guildmanager.GuildManager;
import com.mcatk.guildmanager.core.service.GuildService;
import com.mcatk.guildmanager.models.Guild;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

public class GuildCommandTest {
    @Test public void quitCleansUpOnceBeforeAnyScheduledRefreshRuns() {
        GuildManager plugin = mock(GuildManager.class);
        GuildService service = mock(GuildService.class);
        Player player = mock(Player.class);
        Guild guild = new Guild();
        guild.setId("ac");
        when(player.getName()).thenReturn("Alice");
        when(player.isOnline()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        when(plugin.getGuildService()).thenReturn(service);
        when(service.isReady()).thenReturn(true);
        when(service.getPlayerGuild("Alice")).thenReturn(guild);
        when(service.removeMember("Alice", "ac")).thenReturn(true, false);
        doAnswer(call -> {
            java.util.function.Supplier<Boolean> work = call.getArgument(0);
            java.util.function.Consumer<Boolean> success = call.getArgument(1);
            success.accept(work.get());
            return null;
        }).when(plugin).queryAsync(any(), any(), any());
        org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        org.bukkit.command.ConsoleCommandSender console = mock(org.bukkit.command.ConsoleCommandSender.class);
        try (MockedStatic<GuildManager> global = mockStatic(GuildManager.class);
             MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            global.when(GuildManager::getPlugin).thenReturn(plugin);
            bukkit.when(org.bukkit.Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(org.bukkit.Bukkit::getConsoleSender).thenReturn(console);
            String revoke = "res pset main.gh Alice move remove";
            bukkit.when(() -> org.bukkit.Bukkit.dispatchCommand(console, revoke)).thenReturn(true);
            GuildCommand handler = new GuildCommand();
            handler.onCommand(player, mock(Command.class), "gmg", new String[]{"quit"});
            handler.onCommand(player, mock(Command.class), "gmg", new String[]{"quit"});
            bukkit.verify(() -> org.bukkit.Bukkit.dispatchCommand(console, revoke), times(1));
            verify(player, times(1)).sendMessage("§a已退出公会");
            verify(scheduler, times(2)).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        }
    }

    @Test public void existingIdCannotReachPaymentService() {
        GuildManager plugin=mock(GuildManager.class);GuildService service=mock(GuildService.class);
        Player player=mock(Player.class);when(player.getName()).thenReturn("Alice");
        when(plugin.getGuildService()).thenReturn(service);when(service.isReady()).thenReturn(true);
        when(service.getGuild("existing")).thenReturn(new Guild());
        try(MockedStatic<GuildManager> global=mockStatic(GuildManager.class)) {
            global.when(GuildManager::getPlugin).thenReturn(plugin);
            new GuildCommand().onCommand(player,mock(Command.class),"gmg",new String[]{"create","existing"});
            verify(plugin,never()).getPayments();verify(plugin,never()).takePlayerMoney(any(),anyDouble());
        }
    }
}
