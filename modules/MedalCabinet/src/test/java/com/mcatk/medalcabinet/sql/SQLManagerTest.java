package com.mcatk.medalcabinet.sql;

import com.mcatk.medalcabinet.medal.Medal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQLManagerTest {

    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    private SQLManager sqlManager;

    @BeforeEach
    void setUp() throws SQLException {
        sqlManager = new SQLManager(connection);
        SQLManager.setInstance(sqlManager);

        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        lenient().when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void testGetMainMedalCached() throws SQLException {
        // Setup mock to return a medal ID "medal1" for player "player1"

        // Simulating:
        // First query (get main medal ID) returns "medal1"
        // Second query (get medal details) returns medal details

        when(resultSet.next()).thenReturn(true, true); // Sufficient for 1 call (2 queries)
        when(resultSet.getString("medal_id")).thenReturn("medal1");
        when(resultSet.getString("medal_name")).thenReturn("Medal One");

        // Call 1 - Should hit DB
        Medal m1 = sqlManager.getMainMedal("player1");
        assertNotNull(m1);
        assertEquals("Medal One", m1.getName());

        // Call 2 - Should NOT hit DB (cached)
        Medal m2 = sqlManager.getMainMedal("player1");
        assertNotNull(m2);
        assertEquals("Medal One", m2.getName());
        assertEquals(m1, m2); // Should be same object reference if cached

        // Verify that prepareStatement was called exactly 2 times (for the first call only)
        verify(connection, times(2)).prepareStatement(anyString());

        // Clear cache
        sqlManager.clearCache("player1");

        // Setup mock for another call
        when(resultSet.next()).thenReturn(true, true);
        when(resultSet.getString("medal_id")).thenReturn("medal1");
        when(resultSet.getString("medal_name")).thenReturn("Medal One");

        // Call 3 - Should hit DB again
        Medal m3 = sqlManager.getMainMedal("player1");
        assertNotNull(m3);

        // Verify prepareStatement called 2 more times (total 4)
        verify(connection, times(4)).prepareStatement(anyString());
    }

    @Test
    void testGetMainMedalNullCaching() throws SQLException {
        // Setup mock to return no medal (false on first next())
        when(resultSet.next()).thenReturn(false);

        // Call 1 - Should hit DB (1 query)
        Medal m1 = sqlManager.getMainMedal("noplayer");
        assertNull(m1);

        // Call 2 - Should NOT hit DB (cached empty)
        Medal m2 = sqlManager.getMainMedal("noplayer");
        assertNull(m2);

        // Verify query count.
        // Logic:
        // 1. SELECT medal_id ... returns empty (resultSet.next() -> false).
        // 2. Returns null.
        // 3. Cache put(noplayer, EMPTY_MEDAL).

        // So only 1 query "SELECT medal_id..." executed.
        verify(connection, times(1)).prepareStatement(anyString());
    }
}
