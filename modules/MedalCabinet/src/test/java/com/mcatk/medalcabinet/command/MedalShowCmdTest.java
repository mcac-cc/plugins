package com.mcatk.medalcabinet.command;

import com.mcatk.medalcabinet.MedalCabinet;
import com.mcatk.medalcabinet.medal.Medal;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MedalShowCmdTest {

    private MockedStatic<Bukkit> mockedBukkit;
    private MockedStatic<SQLManager> mockedSQLManagerStatic;
    private MockedStatic<MedalCabinet> mockedMedalCabinetStatic;
    private SQLManager mockedSQLManager;
    private MedalCabinet mockedPlugin;
    private Server mockedServer;
    private BukkitScheduler mockedScheduler;
    private Player mockedPlayer;

    @BeforeEach
    public void setUp() {
        mockedBukkit = mockStatic(Bukkit.class);
        mockedSQLManagerStatic = mockStatic(SQLManager.class);
        mockedMedalCabinetStatic = mockStatic(MedalCabinet.class);

        mockedPlugin = mock(MedalCabinet.class);
        mockedServer = mock(Server.class);
        mockedScheduler = mock(BukkitScheduler.class);
        mockedPlayer = mock(Player.class);
        mockedSQLManager = mock(SQLManager.class);

        // Setup Bukkit statics
        mockedBukkit.when(Bukkit::getServer).thenReturn(mockedServer);
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(mockedScheduler);

        // Setup SQLManager singleton
        mockedSQLManagerStatic.when(SQLManager::getInstance).thenReturn(mockedSQLManager);

        // Setup Plugin
        mockedMedalCabinetStatic.when(MedalCabinet::getPlugin).thenReturn(mockedPlugin);
        when(mockedPlugin.getServer()).thenReturn(mockedServer);

        // Setup Player
        when(mockedPlayer.getName()).thenReturn("TestPlayer");
    }

    @AfterEach
    public void tearDown() {
        mockedBukkit.close();
        mockedSQLManagerStatic.close();
        mockedMedalCabinetStatic.close();
    }

    @Test
    public void testShowAll() {
        MedalShowCmd cmd = new MedalShowCmd();
        String[] args = {"all"};

        // Mock SQL return
        ArrayList<Medal> medals = new ArrayList<>();
        medals.add(new Medal("1", "TestMedal", "STONE", "Desc"));
        when(mockedSQLManager.getPlayerMedals("TestPlayer")).thenReturn(medals);

        // Capture the async task
        ArgumentCaptor<Runnable> asyncTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(mockedScheduler.runTaskAsynchronously(eq(mockedPlugin), asyncTaskCaptor.capture()))
                .thenReturn(mock(BukkitTask.class));

        // Execute command
        cmd.onCommand(mockedPlayer, mock(Command.class), "medalshow", args);

        // Run the captured async task
        Runnable asyncTask = asyncTaskCaptor.getValue();
        asyncTask.run();

        // Verify that runTask (sync) was called
        ArgumentCaptor<Runnable> syncTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockedScheduler).runTask(eq(mockedPlugin), syncTaskCaptor.capture());

        // Run the captured sync task
        Runnable syncTask = syncTaskCaptor.getValue();
        syncTask.run();

        // Verify broadcastMessage is called
        verify(mockedServer).broadcastMessage(contains("TestPlayer 展示了他的勋章"));
    }
}
