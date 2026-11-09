package com.mcatk.guildmanager;

import com.mcatk.guildmanager.core.service.GuildService;
import com.mcatk.guildmanager.core.repository.jdbc.GuildTransactionException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.*;
import org.mockito.MockedStatic;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GuildPaymentsTest {
    private MockedStatic<Bukkit> bukkit;
    private GuildManager plugin;
    private GuildService service;
    private Player player;
    private GuildPayments payments;
    private Path folder;
    private final BlockingQueue<Runnable> main=new LinkedBlockingQueue<>();
    private final Thread owner=Thread.currentThread();
    @Before public void setUp() throws Exception {
        folder=Files.createTempDirectory("guild-payments-test");
        plugin=mock(GuildManager.class); service=mock(GuildService.class);player=mock(Player.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.isEnabled()).thenReturn(true);when(plugin.getGuildService()).thenReturn(service);
        when(player.getName()).thenReturn("Alice");when(player.getUniqueId()).thenReturn(UUID.randomUUID());when(player.isOnline()).thenReturn(true);
        when(plugin.takePlayerMoney(player,1000000)).thenAnswer(call->{assertSame(owner,Thread.currentThread());return true;});
        when(plugin.refundPlayerMoney(player,1000000)).thenAnswer(call->{assertSame(owner,Thread.currentThread());return true;});
        BukkitScheduler scheduler=mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(plugin),any(Runnable.class))).thenAnswer(call->{main.add(call.getArgument(1));return null;});
        bukkit=mockStatic(Bukkit.class);bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        payments=new GuildPayments(plugin);
    }
    @After public void tearDown() {payments.close();bukkit.close();}
    private void next() throws Exception {Runnable action=main.poll(5,TimeUnit.SECONDS);assertNotNull("main callback",action);action.run();}
    private boolean receipt(String state) throws Exception {
        Path directory=folder.resolve("pending-payments");if(!Files.isDirectory(directory))return false;
        try(java.util.stream.Stream<Path> files=Files.list(directory)) {
            for(Path path:(Iterable<Path>)files::iterator) if(path.toString().endsWith(".properties")) {
                Properties record=new Properties();try(java.io.InputStream in=Files.newInputStream(path)){record.load(in);}
                if(state.equals(record.getProperty("state")))return true;
            }
        }return false;
    }
    @Test public void duplicateIdFailsPreflightWithoutChargingOrCommitting() throws Exception {
        payments.charge(player,"create:ac",1000000,()->false,()->fail("must not commit"),"success");
        next();payments.close();verify(plugin,never()).takePlayerMoney(any(),anyDouble());
    }
    @Test public void vaultAndUiUseMainWhileSqlUsesWorker() throws Exception {
        AtomicBoolean committed=new AtomicBoolean();
        payments.charge(player,"create:ac",1000000,()->{assertNotSame(owner,Thread.currentThread());return true;},()->{
            assertNotSame(owner,Thread.currentThread());committed.set(true);
        },"success");
        next();next();payments.close();assertTrue(committed.get());
        verify(player).sendMessage("success");verify(plugin,never()).refundPlayerMoney(any(),anyDouble());
    }
    @Test public void rolledBackSqlFailureRefundsExactlyOnce() throws Exception {
        payments.charge(player,"create:ac",1000000,()->true,()->{
            throw new GuildTransactionException("Second insert failed",new RuntimeException(),false);
        },"success");
        next();next();payments.close();verify(plugin).refundPlayerMoney(player,1000000);
        verify(player,never()).sendMessage("success");
    }
    @Test public void unknownCommitKeepsEvidenceAndDoesNotBlindlyRefund() throws Exception {
        payments.charge(player,"create:ac",1000000,()->true,()->{
            throw new GuildTransactionException("Lost commit response",new RuntimeException(),true);
        },"success");
        next();next();payments.close();assertTrue(receipt("COMMIT_UNKNOWN"));
        verify(plugin,never()).refundPlayerMoney(any(),anyDouble());
        payments=new GuildPayments(plugin);
        payments.charge(player,"create:another",1000000,()->true,()->fail("blocked"),"success");
        payments.close();verify(plugin).takePlayerMoney(player,1000000);
    }
    @Test public void failedCacheRefreshAfterCommitNeverRefundsACompletedCreation() throws Exception {
        doThrow(new IllegalStateException("Read failed")).when(service).refresh();
        payments.charge(player,"create:ac",1000000,()->true,()->{},"success");
        next();next();payments.close();verify(player).sendMessage("success");
        verify(plugin,never()).refundPlayerMoney(any(),anyDouble());
    }
    @Test public void shutdownCompensatesKnownFailureEvenIfItsMainCallbackHasNotRun() throws Exception {
        payments.charge(player,"create:ac",1000000,()->true,()->{
            throw new GuildTransactionException("Rollback",new RuntimeException(),false);
        },"success");
        next();Runnable refund=main.poll(5,TimeUnit.SECONDS);assertNotNull(refund);
        payments.close();refund.run();verify(plugin).refundPlayerMoney(player,1000000);
    }
    @Test public void missingTransactionalTableSupportRejectsBeforeCharging() throws Exception {
        doThrow(new IllegalStateException("MyISAM is not supported")).when(service).validatePayments();
        payments.charge(player,"create:ac",1000000,()->true,()->fail("must not commit"),"success");
        next();payments.close();verify(plugin,never()).takePlayerMoney(any(),anyDouble());
    }
}
