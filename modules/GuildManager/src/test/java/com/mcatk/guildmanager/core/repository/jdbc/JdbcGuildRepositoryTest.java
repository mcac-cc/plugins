package com.mcatk.guildmanager.core.repository.jdbc;

import com.mcatk.guildmanager.core.config.DbConfig;
import org.junit.Test;
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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class JdbcGuildRepositoryTest {
    private final DbConfig config = new DbConfig("localhost", 3306, "test", "test", "test");

    @Test
    public void reconnectsBeforeReadingAnExpiredConnection() throws Exception {
        Connection stale = mock(Connection.class);
        Connection fresh = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(fresh.isValid(2)).thenReturn(true);
        when(fresh.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getString("g.guild_id")).thenReturn("guild");
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(config.toJdbcUrl(), "test", "test")).thenReturn(stale, fresh);
            JdbcGuildRepository repository = new JdbcGuildRepository(config);
            assertEquals("guild", repository.getPlayerGuildId("player"));
            assertEquals("guild", repository.getPlayerGuildId("player"));
            verify(stale).close();
            verify(stale, never()).prepareStatement(anyString());
            verify(result, times(2)).close();
        }
    }

    @Test
    public void validationExceptionAndClosedConnectionAreBothReplaced() throws Exception {
        SQLException failure1 = new SQLException("Connection reset");
        Connection stale = mock(Connection.class);
        Connection closed = mock(Connection.class);
        Connection fresh = mock(Connection.class);
        when(stale.isValid(2)).thenThrow(failure1);
        when(closed.isClosed()).thenReturn(true);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(closed.prepareStatement(anyString())).thenReturn(statement);
        when(fresh.prepareStatement(anyString())).thenReturn(statement);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(config.toJdbcUrl(), "test", "test")).thenReturn(stale, closed, fresh);
            JdbcGuildRepository repository = new JdbcGuildRepository(config);
            repository.updateMember(mock(com.mcatk.guildmanager.models.Member.class));
            repository.updateMember(mock(com.mcatk.guildmanager.models.Member.class));
            verify(stale).close();
            verify(closed).close();
            verify(fresh).prepareStatement(anyString());
        }
    }

    @Test
    public void failedReconnectIsNotReportedAsAMissingGuildAndCanRecover() throws Exception {
        SQLException failure1 = new SQLException("Unavailable");
        Connection stale = mock(Connection.class);
        Connection fresh = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(fresh.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getString("g.guild_id")).thenReturn("guild");
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(config.toJdbcUrl(), "test", "test"))
                    .thenReturn(stale).thenThrow(failure1).thenReturn(fresh);
            JdbcGuildRepository repository = new JdbcGuildRepository(config);
            assertThrows(IllegalStateException.class, () -> repository.getPlayerGuildId("player"));
            assertEquals("guild", repository.getPlayerGuildId("player"));
        }
    }

    @Test
    public void queryFailureCannotReplaceGuildCacheWithEmptyResults() throws Exception {
        SQLException failure1 = new SQLException("Connection lost", "08S01");
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.isValid(2)).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(failure1);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(config.toJdbcUrl(), "test", "test")).thenReturn(connection);
            JdbcGuildRepository repository = new JdbcGuildRepository(config);
            assertThrows(IllegalStateException.class, repository::getAllGuilds);
            verify(statement).close();
        }
    }

    @Test
    public void ambiguousWriteIsNotRetriedAndShutdownPreventsReopening() throws Exception {
        SQLException failure1 = new SQLException("Lost response after commit", "08S01");
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.isValid(2)).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(failure1);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(config.toJdbcUrl(), "test", "test")).thenReturn(connection);
            JdbcGuildRepository repository = new JdbcGuildRepository(config);
            assertThrows(IllegalStateException.class, () -> repository.createGuild("guild", "player"));
            verify(statement).executeUpdate();
            repository.close();
            repository.close();
            assertThrows(IllegalStateException.class, () -> repository.createGuild("guild", "player"));
            driver.verify(() -> DriverManager.getConnection(config.toJdbcUrl(), "test", "test"), times(1));
            verify(connection).close();
        }
    }
    @Test
    public void sharedConnectionCannotBeReplacedWhileAnotherOperationIsRunning() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.isValid(2)).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch releaseQuery = new CountDownLatch(1);
        CountDownLatch writeStarted = new CountDownLatch(1);
        when(statement.executeQuery()).thenAnswer(invocation -> {
            queryStarted.countDown();
            assertTrue(releaseQuery.await(5, TimeUnit.SECONDS));
            return mock(ResultSet.class);
        });
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(config.toJdbcUrl(), "test", "test")).thenReturn(connection);
            JdbcGuildRepository repository = new JdbcGuildRepository(config);
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                Future<?> query = workers.submit(() -> repository.getPlayerGuildId("player"));
                assertTrue(queryStarted.await(5, TimeUnit.SECONDS));
                Future<?> write = workers.submit(() -> {
                    writeStarted.countDown();
                    repository.createGuild("guild", "player");
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

}
