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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SQLManagerTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement1;
    @Mock
    private PreparedStatement preparedStatement2;

    @Mock
    private ResultSet resultSet1;
    @Mock
    private ResultSet resultSet2;

    private SQLManager sqlManager;

    @BeforeEach
    public void setUp() {
        // Use the protected constructor to inject the mock connection
        // We can do this because the test class is in the same package
        sqlManager = new SQLManager(connection);
        // Set the singleton instance
        SQLManager.setInstance(sqlManager);
    }

    @Test
    public void testGetMainMedal_Optimized() throws SQLException {
        // Arrange
        String playerId = "testPlayer";
        String medalId = "medal123";
        String medalName = "Gold Medal";
        String medalMat = "GOLD_INGOT";
        String medalDesc = "A shiny gold medal";

        // Mock the JOIN query: SELECT m.* FROM medal m INNER JOIN player_main_medal pmm ...
        when(connection.prepareStatement(contains("INNER JOIN"))).thenReturn(preparedStatement1);
        when(preparedStatement1.executeQuery()).thenReturn(resultSet1);
        when(resultSet1.next()).thenReturn(true);
        when(resultSet1.getString("medal_id")).thenReturn(medalId);
        when(resultSet1.getString("medal_name")).thenReturn(medalName);
        when(resultSet1.getString("medal_material")).thenReturn(medalMat);
        when(resultSet1.getString("medal_description")).thenReturn(medalDesc);

        // Act
        Medal result = sqlManager.getMainMedal(playerId);

        // Assert
        assertNotNull(result);
        assertEquals(medalId, result.getId());
        assertEquals(medalName, result.getName());

        // Verify that prepareStatement was called only once (optimized behavior)
        verify(connection, times(1)).prepareStatement(anyString());

        // Specific verification
        verify(connection).prepareStatement(contains("INNER JOIN"));
    }
}
