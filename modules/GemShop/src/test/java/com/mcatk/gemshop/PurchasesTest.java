package com.mcatk.gemshop;

import com.mcatk.gem.GemExecutor;
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

public class PurchasesTest {
    private MockedStatic<Bukkit> bukkit;
    private GemShop plugin;
    private GemExecutor gems;
    private Player player;
    private Purchases purchases;
    private Path folder;
    private final BlockingQueue<Runnable> main = new LinkedBlockingQueue<>();
    private final Thread owner = Thread.currentThread();
    @Before public void setUp() throws Exception {
        folder=Files.createTempDirectory("gem-purchases-test");
        plugin=mock(GemShop.class); gems=mock(GemExecutor.class); player=mock(Player.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile()); when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.isEnabled()).thenReturn(true);
        when(player.getName()).thenReturn("Alice"); when(player.getUniqueId()).thenReturn(UUID.randomUUID()); when(player.isOnline()).thenReturn(true);
        when(gems.isReady()).thenReturn(true);
        when(gems.takeGems(anyString(),anyInt())).thenAnswer(call->{ assertNotSame(owner,Thread.currentThread()); return true; });
        when(gems.refundGems(anyString(),anyInt())).thenAnswer(call->{ assertNotSame(owner,Thread.currentThread()); return true; });
        BukkitScheduler scheduler=mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(plugin),any(Runnable.class))).thenAnswer(call->{main.add(call.getArgument(1));return null;});
        bukkit=mockStatic(Bukkit.class); bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        purchases=new Purchases(plugin,gems);
    }
    @After public void tearDown() { purchases.close(); bukkit.close(); }
    private void next() throws Exception { Runnable action=main.poll(5,TimeUnit.SECONDS);assertNotNull("main-thread callback",action);action.run(); }
    private boolean receipt(String state) throws Exception {
        Path directory=folder.resolve("pending-purchases"); if(!Files.isDirectory(directory))return false;
        try(java.util.stream.Stream<Path> files=Files.list(directory)) {
            for(Path path:(Iterable<Path>)files::iterator) if(path.toString().endsWith(".properties")) {
                Properties p=new Properties();try(java.io.InputStream in=Files.newInputStream(path)){p.load(in);}
                if(state.equals(p.getProperty("state")))return true;
            }
        }return false;
    }
    @Test public void failedCapacityCheckNeverCharges() {
        purchases.buy(player,10,()->false,()->{fail("delivery must not run");return false;});
        purchases.close(); verify(gems,never()).takeGems(anyString(),anyInt());
    }
    @Test public void inventoryChangingWhileSqlRunsRefundsOnceWithoutLifetimeAward() throws Exception {
        AtomicBoolean fits=new AtomicBoolean(true);
        purchases.buy(player,10,fits::get,()->{fail("delivery must not run");return false;});
        fits.set(false); next(); purchases.close();
        verify(gems).takeGems("Alice",10); verify(gems).refundGems("Alice",10);
        verify(gems,never()).addGems(anyString(),anyInt()); assertFalse(receipt("REFUND_PENDING"));
    }
    @Test public void successfulDeliveryRunsOnMainAndDoesNotRefund() throws Exception {
        purchases.buy(player,10,()->true,()->{assertSame(owner,Thread.currentThread());return true;});
        next(); purchases.close(); verify(gems,never()).refundGems(anyString(),anyInt());
        verify(player).sendMessage("§a购买成功");
    }
    @Test public void disconnectBeforeDeliveryRefunds() throws Exception {
        purchases.buy(player,10,()->true,()->{fail("offline delivery");return true;});
        when(player.isOnline()).thenReturn(false); next(); purchases.close(); verify(gems).refundGems("Alice",10);
    }
    @Test public void shutdownBeforeTheMainCallbackRefundsAndLateCallbackCannotDeliver() throws Exception {
        AtomicBoolean delivered=new AtomicBoolean();
        purchases.buy(player,10,()->true,()->{delivered.set(true);return true;});
        Runnable callback=main.poll(5,TimeUnit.SECONDS); assertNotNull(callback);
        purchases.close(); callback.run();
        assertFalse(delivered.get()); verify(gems).refundGems("Alice",10);
    }
    @Test public void concurrentClicksCannotChargeTwice() throws Exception {
        purchases.buy(player,10,()->true,()->true);
        purchases.buy(player,10,()->true,()->true);
        next(); purchases.close(); verify(gems).takeGems("Alice",10);
    }
    @Test public void ambiguousDebitKeepsReceiptAndIsNotBlindlyRefundedOrRetried() throws Exception {
        doThrow(new IllegalStateException("Lost reply after debit")).when(gems).takeGems("Alice",10);
        purchases.buy(player,10,()->true,()->true); next(); purchases.close();
        assertTrue(receipt("DEBIT_UNKNOWN")); verify(gems,never()).refundGems(anyString(),anyInt());
        purchases=new Purchases(plugin,gems);
        purchases.buy(player,10,()->true,()->true); purchases.close(); verify(gems).takeGems("Alice",10);
    }
    @Test public void failedRefundRetainsReceiptInsteadOfReportingSuccess() throws Exception {
        doReturn(false).when(gems).refundGems("Alice",10);
        purchases.buy(player,10,()->true,()->false); next(); purchases.close();
        assertTrue(receipt("REFUND_UNKNOWN")); verify(gems).refundGems("Alice",10);
        verify(player,never()).sendMessage("§a购买成功");
    }
}
