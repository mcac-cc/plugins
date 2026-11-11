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
