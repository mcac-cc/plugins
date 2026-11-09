package com.mcatk.medalcabinet.sql;

import com.mcatk.medalcabinet.medal.Medal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SQLManagerTest {
    private static final String URL = "jdbc:mysql://localhost/test";
    private SQLManager manager;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet result;
    private MockedStatic<DriverManager> driver;

    @BeforeEach
    void setUp() throws SQLException {
        manager = new SQLManager(URL, "test", "test");
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        result = mock(ResultSet.class);
        when(connection.isValid(2)).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        driver = mockStatic(DriverManager.class);
        driver.when(() -> DriverManager.getConnection(URL, "test", "test")).thenReturn(connection);
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    private void returnMedal(String name) throws SQLException {
        when(result.next()).thenReturn(true, false);
        when(result.getString("medal_id")).thenReturn("medal1");
        when(result.getString("medal_name")).thenReturn(name);
    }

    @Test
    void cachesSuccessfulMedalReadsAndSupportsInvalidation() throws SQLException {
        returnMedal("Medal One");
        Medal first = manager.getMainMedal("player");
        assertEquals("Medal One", first.getName());
        assertSame(first, manager.getMainMedal("player"));
        verify(statement).executeQuery();
        manager.clearCache("player");
        returnMedal("Medal One");
        assertEquals("Medal One", manager.getMainMedal("player").getName());
        verify(statement, times(2)).executeQuery();
        verify(result, times(2)).close();
    }

    @Test
    void cachesAConfirmedMissingMedal() throws SQLException {
        when(result.next()).thenReturn(false);
        assertNull(manager.getMainMedal("player"));
        assertNull(manager.getMainMedal("player"));
        verify(statement).executeQuery();
    }

    @Test
    void reconnectsBeforeReadingAnExpiredConnection() throws SQLException {
        returnMedal("Medal One");
        manager.getPlayerMedals("player");
        Connection fresh = mock(Connection.class);
        when(fresh.prepareStatement(anyString())).thenReturn(statement);
        when(connection.isValid(2)).thenReturn(false);
        driver.when(() -> DriverManager.getConnection(URL, "test", "test")).thenReturn(fresh);
        returnMedal("Medal One");
        assertEquals("Medal One", manager.getMainMedal("player").getName());
        verify(connection).close();
        verify(fresh).prepareStatement(anyString());
    }

    @Test
    void reconnectsWhenValidationThrowsOrConnectionIsClosed() throws SQLException {
        SQLException failure1 = new SQLException("Connection reset");
        SQLException failure2 = new SQLException("Already broken");
        manager.getAllMedals();
        Connection second = mock(Connection.class);
        Connection third = mock(Connection.class);
        when(second.prepareStatement(anyString())).thenReturn(statement);
        when(third.prepareStatement(anyString())).thenReturn(statement);
        when(connection.isValid(2)).thenThrow(failure1);
        doThrow(failure2).when(connection).close();
        driver.when(() -> DriverManager.getConnection(URL, "test", "test")).thenReturn(second, third);
        manager.getAllMedals();
        when(second.isClosed()).thenReturn(true);
        manager.getAllMedals();
        verify(connection).close();
        verify(second).close();
        verify(third).prepareStatement(anyString());
    }

    @Test
    void failedQueryDoesNotPoisonTheEmptyMedalCache() throws SQLException {
        SQLException failure1 = new SQLException("Connection lost", "08S01");
        when(statement.executeQuery()).thenThrow(failure1).thenReturn(result);
        returnMedal("Recovered Medal");
        assertNull(manager.getMainMedal("player"));
        assertEquals("Recovered Medal", manager.getMainMedal("player").getName());
    }

    @Test
    void failedReconnectDoesNotPoisonTheEmptyMedalCache() throws SQLException {
        SQLException failure1 = new SQLException("Database unavailable");
        driver.when(() -> DriverManager.getConnection(URL, "test", "test"))
                .thenThrow(failure1).thenReturn(connection);
        returnMedal("Recovered Medal");
        assertNull(manager.getMainMedal("player"));
        assertEquals("Recovered Medal", manager.getMainMedal("player").getName());
    }

    @Test
    void doesNotReplayAnAmbiguousWrite() throws SQLException {
        SQLException failure1 = new SQLException("Lost response after commit", "08S01");
        when(statement.executeUpdate()).thenThrow(failure1);
        manager.createMedal("id", "name", "STONE", "description");
        verify(statement).executeUpdate();
        verify(statement).close();
        driver.verify(() -> DriverManager.getConnection(URL, "test", "test"), times(1));
    }

    @Test
    void failedOwnershipQueryCannotProceedToGrantAMedal() throws SQLException {
        SQLException failure1 = new SQLException("Database unavailable");
        when(statement.executeQuery()).thenThrow(failure1);
        manager.addPlayerMedal("player", "medal");
        verify(statement, never()).executeUpdate();
    }

    @Test
    void changingMainMedalInvalidatesTheCachedSelection() throws SQLException {
        returnMedal("Old Medal");
        assertEquals("Old Medal", manager.getMainMedal("player").getName());
        when(statement.executeUpdate()).thenReturn(1);
        when(result.next()).thenReturn(true);
        assertTrue(manager.setMainMedal("player", "medal1"));
        when(result.getString("medal_name")).thenReturn("New Medal");
        assertEquals("New Medal", manager.getMainMedal("player").getName());
    }

    @Test
    void shutdownIsIdempotentAndPreventsReopening() throws SQLException {
        manager.getAllMedals();
        manager.shutdown();
        manager.shutdown();
        assertNull(manager.getMainMedal("player"));
        verify(connection).close();
        driver.verify(() -> DriverManager.getConnection(URL, "test", "test"), times(1));
    }

    @Test
    void anotherOperationCannotUseOrReplaceAConnectionDuringAQuery() throws Exception {
        manager.getAllMedals();
        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch releaseQuery = new CountDownLatch(1);
        CountDownLatch writeStarted = new CountDownLatch(1);
        when(statement.executeQuery()).thenAnswer(invocation -> {
            queryStarted.countDown();
            assertTrue(releaseQuery.await(5, TimeUnit.SECONDS));
            return result;
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> query = workers.submit(() -> manager.getPlayerMedals("player"));
            assertTrue(queryStarted.await(5, TimeUnit.SECONDS));
            Future<?> write = workers.submit(() -> {
                writeStarted.countDown();
                manager.createMedal("id", "name", "STONE", "description");
            });
            assertTrue(writeStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> write.get(100, TimeUnit.MILLISECONDS));
            verify(statement, never()).executeUpdate();
            releaseQuery.countDown();
            query.get(5, TimeUnit.SECONDS);
            write.get(5, TimeUnit.SECONDS);
            verify(statement).executeUpdate();
        } finally {
            releaseQuery.countDown();
            workers.shutdownNow();
        }
    }
}
