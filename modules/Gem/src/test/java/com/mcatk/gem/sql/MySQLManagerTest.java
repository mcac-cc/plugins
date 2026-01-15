package com.mcatk.gem.sql;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MySQLManagerTest {

    @Mock
    private Connection connectionMock;
    @Mock
    private PreparedStatement preparedStatementMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddGems_SQL() throws Exception {
        MySQLManager manager = new MySQLManager();

        // Inject mock connection
        Field connectionField = MySQLManager.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(manager, connectionMock);

        when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);

        String name = "user1";
        int amount = 50;

        manager.addGems(name, amount);

        // Verify the SQL statement
        String expectedSQL = "INSERT INTO `gem` (`username`, `gems`, `total`) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `gems` = `gems` + ?, `total` = `total` + ?";
        verify(connectionMock).prepareStatement(expectedSQL);

        // Verify parameters
        verify(preparedStatementMock).setString(1, name);
        verify(preparedStatementMock).setInt(2, amount); // gems value
        verify(preparedStatementMock).setInt(3, amount); // total value
        verify(preparedStatementMock).setInt(4, amount); // increment gems
        verify(preparedStatementMock).setInt(5, amount); // increment total

        verify(preparedStatementMock).executeUpdate();
    }
}
