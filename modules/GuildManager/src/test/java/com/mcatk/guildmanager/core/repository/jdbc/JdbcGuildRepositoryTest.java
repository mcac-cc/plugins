package com.mcatk.guildmanager.core.repository.jdbc;

import com.mcatk.guildmanager.core.config.DbConfig;
import com.mcatk.guildmanager.models.Guild;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class JdbcGuildRepositoryTest {

    private JdbcGuildRepository repository;
    private Connection mockConnection;
    private ResultSet mockResultSetGuilds;
    private ResultSet mockResultSetMembers;

    @Before
    public void setUp() throws Exception {
        DbConfig config = new DbConfig("localhost", 3306, "test", "user", "pass");
        repository = new JdbcGuildRepository(config);

        mockConnection = mock(Connection.class);
        mockResultSetGuilds = mock(ResultSet.class);
        mockResultSetMembers = mock(ResultSet.class);

        // Inject mock connection
        Field connectionField = JdbcGuildRepository.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(repository, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
    }

    @Test
    public void testGetAllGuilds_Optimization() throws SQLException {
        PreparedStatement psGuilds = mock(PreparedStatement.class);
        PreparedStatement psMembers = mock(PreparedStatement.class);

        // Setup mock calls for prepareStatement
        when(mockConnection.prepareStatement(contains("FROM guild"))).thenReturn(psGuilds);
        // The optimized code fetches all members at once
        when(mockConnection.prepareStatement(contains("SELECT * FROM player_guild"))).thenReturn(psMembers);

        when(psGuilds.executeQuery()).thenReturn(mockResultSetGuilds);
        when(psMembers.executeQuery()).thenReturn(mockResultSetMembers);

        // Setup 5 guilds
        when(mockResultSetGuilds.next()).thenReturn(true, true, true, true, true, false);
        when(mockResultSetGuilds.getString("guild_id")).thenReturn("g1", "g2", "g3", "g4", "g5");

        // Members return nothing for now
        when(mockResultSetMembers.next()).thenReturn(true, true, false);
        when(mockResultSetMembers.getString("player_id")).thenReturn("player1", "player2");
        when(mockResultSetMembers.getString("guild_id")).thenReturn("g1", "g2");
        when(mockResultSetMembers.getInt("player_contribution")).thenReturn(100, 200);
        when(mockResultSetMembers.getBoolean("player_is_advanced")).thenReturn(true, false);

        HashMap<String, Guild> result = repository.getAllGuilds();

        assertEquals(5, result.size());

        // Verify optimization:
        // 1 call for guilds
        // 1 call for all members
        // Total 2 calls to prepareStatement
        verify(mockConnection, times(2)).prepareStatement(anyString());

        // Verify Data Mapping
        assertEquals(1, result.get("g1").getMembers().size());
        assertEquals("player1", result.get("g1").getMembers().get(0).getId());

        assertEquals(1, result.get("g2").getMembers().size());
        assertEquals("player2", result.get("g2").getMembers().get(0).getId());

        assertEquals(0, result.get("g3").getMembers().size());
    }
}
