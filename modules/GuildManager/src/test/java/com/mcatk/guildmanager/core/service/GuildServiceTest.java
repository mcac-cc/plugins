package com.mcatk.guildmanager.core.service;

import com.mcatk.guildmanager.core.repository.GuildRepository;
import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.Member;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GuildServiceTest {
    private HashMap<String,Guild> sample() {
        Guild guild=new Guild();guild.setId("ac");guild.setGuildName("AC");
        guild.getMembers().add(new Member("Alice","ac",10,false));
        HashMap<String,Guild> result=new HashMap<>();result.put("ac",guild);return result;
    }
    @Test public void cachedReadsDoNotQuerySqlOrExposeMutableCacheObjects() {
        GuildRepository repository=mock(GuildRepository.class);when(repository.getAllGuilds()).thenReturn(sample());
        GuildService service=new GuildService(repository);assertFalse(service.isReady());service.refresh();clearInvocations(repository);
        Guild guild=service.getPlayerGuild("alice");assertEquals("ac",guild.getId());guild.setCash(999);
        service.getMember("Alice").setContribution(99);
        assertEquals(0,service.getGuild("ac").getCash());assertEquals(10,service.getMember("alice").getContribution());
        assertEquals(Collections.singletonList("Alice"),service.getGuildMembers("ac"));verifyNoInteractions(repository);
    }
    @Test public void failedRefreshKeepsTheLastCompleteSnapshot() {
        GuildRepository repository=mock(GuildRepository.class);when(repository.getAllGuilds()).thenReturn(sample()).thenThrow(new IllegalStateException("Unavailable"));
        GuildService service=new GuildService(repository);service.refresh();
        assertThrows(IllegalStateException.class,service::refresh);
        assertEquals("ac",service.getPlayerGuild("Alice").getId());
    }
    @Test public void blockedRefreshDoesNotBlockMainThreadReads() throws Exception {
        GuildRepository repository=mock(GuildRepository.class);when(repository.getAllGuilds()).thenReturn(sample());
        GuildService service=new GuildService(repository);service.refresh();
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
        when(repository.getAllGuilds()).thenAnswer(call->{started.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return sample();});
        ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            Future<?> refresh=workers.submit(service::refresh);assertTrue(started.await(5,TimeUnit.SECONDS));
            Future<Guild> read=workers.submit(()->service.getPlayerGuild("Alice"));
            assertEquals("ac",read.get(1,TimeUnit.SECONDS).getId());
            release.countDown();refresh.get(5,TimeUnit.SECONDS);
        } finally {release.countDown();workers.shutdownNow();}
    }
}
