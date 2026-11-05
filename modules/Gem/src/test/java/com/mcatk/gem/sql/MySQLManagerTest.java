package com.mcatk.gem.sql;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MySQLManagerTest {
    private MySQLManager manager;
    private Connection stale;
    private Connection fresh;
    private PreparedStatement statement;

    @Before
    public void setUp() throws Exception {
        manager = new MySQLManager();
        stale = mock(Connection.class);
        fresh = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        setField("enabled", true);
        setField("connection", stale);
        when(fresh.isValid(2)).thenReturn(true);
        when(fresh.prepareStatement(anyString())).thenReturn(statement);
        when(stale.prepareStatement(anyString())).thenReturn(statement);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = MySQLManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(manager, value);
    }

    @Test
    public void reconnectsBeforeReadingAnExpiredConnection() throws Exception {
        ResultSet result = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getInt("gems")).thenReturn(42);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(anyString(), any(), any())).thenReturn(fresh);
            assertEquals(Integer.valueOf(42), manager.getGems("player"));
            assertEquals(Integer.valueOf(42), manager.getGems("player"));
            driver.verify(() -> DriverManager.getConnection(anyString(), any(), any()), times(1));
        }
        verify(stale).close();
        verify(stale, never()).prepareStatement(anyString());
        verify(result, times(2)).close();
    }

    @Test
    public void reconnectsWhenValidationThrows() throws Exception {
        when(stale.isValid(2)).thenThrow(new SQLException("Connection reset"));
        when(statement.executeUpdate()).thenReturn(1);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(anyString(), any(), any())).thenReturn(fresh);
            assertTrue(manager.reduceGems("player", 10));
        }
        verify(stale).close();
        verify(statement, times(1)).executeUpdate();
    }

    @Test
    public void reconnectsWhenConnectionWasClosed() throws Exception {
        when(stale.isClosed()).thenReturn(true);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(anyString(), any(), any())).thenReturn(fresh);
            manager.setTotal("player", 100);
        }
        verify(stale, never()).isValid(anyInt());
        verify(fresh).prepareStatement(anyString());
    }

    @Test
    public void failedReconnectIsNotReportedAsMissingPlayer() throws Exception {
        SQLException unavailable = new SQLException("Database unavailable");
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(anyString(), any(), any()))
                    .thenThrow(unavailable);
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> manager.getData("player"));
            assertTrue(failure.getCause() instanceof SQLException);
        }
        verify(statement, never()).executeQuery();
    }

    @Test
    public void queryFailureIsNotReportedAsMissingPlayer() throws Exception {
        when(stale.isValid(2)).thenReturn(true);
        when(statement.executeQuery()).thenThrow(new SQLException("Connection lost during query"));
        assertThrows(IllegalStateException.class, () -> manager.getData("player"));
        verify(statement).close();
    }

    @Test
    public void doesNotReplayAWriteAfterAnAmbiguousFailure() throws Exception {
        when(stale.isValid(2)).thenReturn(true);
        when(statement.executeUpdate()).thenThrow(new SQLException("Lost response after commit", "08S01"));
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            assertThrows(IllegalStateException.class, () -> manager.reduceGems("player", 10));
            driver.verify(() -> DriverManager.getConnection(anyString(), any(), any()), never());
        }
        verify(statement, times(1)).executeUpdate();
        verify(statement).close();
    }

    @Test
    public void shutdownIsSafeAndPreventsReopening() throws Exception {
        manager.shutdown();
        manager.shutdown();
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            assertThrows(IllegalStateException.class, () -> manager.getGems("player"));
            driver.verify(() -> DriverManager.getConnection(anyString(), any(), any()), never());
        }
        verify(stale, times(1)).close();
        new MySQLManager().shutdown();
    }

    @Test
    public void sharedConnectionIsNotUsedConcurrently() throws Exception {
        when(stale.isValid(2)).thenReturn(true);
        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch releaseQuery = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        when(statement.executeQuery()).thenAnswer(invocation -> {
            queryStarted.countDown();
            assertTrue(releaseQuery.await(5, TimeUnit.SECONDS));
            return mock(ResultSet.class);
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = workers.submit(() -> manager.getGems("player"));
            assertTrue(queryStarted.await(5, TimeUnit.SECONDS));
            Future<?> second = workers.submit(() -> {
                secondStarted.countDown();
                manager.setGems("player", 10);
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            verify(statement, never()).executeUpdate();
            releaseQuery.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            verify(statement).executeUpdate();
        } finally {
            releaseQuery.countDown();
            workers.shutdownNow();
        }
    }
}
