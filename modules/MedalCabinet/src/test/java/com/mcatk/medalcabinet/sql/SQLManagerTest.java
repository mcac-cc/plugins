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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SQLManagerTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement1;

    @Mock
    private ResultSet resultSet1;

    private SQLManager sqlManager;

    @BeforeEach
    public void setUp() {
        sqlManager = new SQLManager(connection);
        SQLManager.setInstance(sqlManager);
    }

    @Test
    public void testGetMainMedal_Optimized() throws SQLException {
        String playerId = "player123";
        String medalId = "medal456";

        // Setup for the single JOIN query
        when(connection.prepareStatement(contains("INNER JOIN"))).thenReturn(preparedStatement1);
        when(preparedStatement1.executeQuery()).thenReturn(resultSet1);
        when(resultSet1.next()).thenReturn(true);
        when(resultSet1.getString("medal_id")).thenReturn(medalId);
        when(resultSet1.getString("medal_name")).thenReturn("Test Medal");
        when(resultSet1.getString("medal_material")).thenReturn("GOLD");
        when(resultSet1.getString("medal_description")).thenReturn("A test medal");

        // Execute
        Medal medal = sqlManager.getMainMedal(playerId);

        // Verify
        assertNotNull(medal);
        assertEquals(medalId, medal.getId());
        assertEquals("Test Medal", medal.getName());

        // Verify that prepareStatement was called only ONCE
        verify(connection, times(1)).prepareStatement(anyString());
        // Verify that it was the JOIN query
        verify(connection).prepareStatement(contains("INNER JOIN"));
    }
}
