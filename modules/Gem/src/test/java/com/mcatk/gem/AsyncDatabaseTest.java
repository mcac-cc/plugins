package com.mcatk.gem;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Test;
import org.mockito.MockedStatic;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncDatabaseTest {
    @Test public void queryRunsOffMainAndReplyWaitsForMainThreadDispatch() throws Exception {
        Thread owner=Thread.currentThread();Gem plugin=mock(Gem.class);
        when(plugin.isEnabled()).thenReturn(true);
        doCallRealMethod().when(plugin).queryAsync(any(),any(),any());
        BukkitScheduler scheduler=mock(BukkitScheduler.class);
        BlockingQueue<Runnable> async=new LinkedBlockingQueue<>(),main=new LinkedBlockingQueue<>();
        when(scheduler.runTaskAsynchronously(eq(plugin),any(Runnable.class))).thenAnswer(call->{async.add(call.getArgument(1));return null;});
        when(scheduler.runTask(eq(plugin),any(Runnable.class))).thenAnswer(call->{main.add(call.getArgument(1));return null;});
        AtomicBoolean replied=new AtomicBoolean();
        try(MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            plugin.queryAsync(()->{assertNotSame(owner,Thread.currentThread());return 42;},value->{
                assertSame(owner,Thread.currentThread());assertEquals(Integer.valueOf(42),value);replied.set(true);
            },error->{throw new AssertionError(error);});
            Runnable query=async.poll(1,TimeUnit.SECONDS);assertNotNull(query);
            Thread worker=new Thread(query);worker.start();worker.join(5000);assertFalse(worker.isAlive());
            assertFalse(replied.get());Runnable reply=main.poll(1,TimeUnit.SECONDS);assertNotNull(reply);reply.run();assertTrue(replied.get());
        }
    }
}
