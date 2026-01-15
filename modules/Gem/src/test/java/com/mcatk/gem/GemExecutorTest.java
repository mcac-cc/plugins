package com.mcatk.gem;

import com.mcatk.gem.sql.MySQLManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GemExecutorTest {

    @Mock
    private Gem gemMock;
    @Mock
    private MySQLManager mysqlManagerMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Mock Gem.plugin
        Field pluginField = Gem.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(null, gemMock);

        // Mock MySQLManager.instance
        Field instanceField = MySQLManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mysqlManagerMock);
    }

    @After
    public void tearDown() throws Exception {
        // Reset static fields
        Field pluginField = Gem.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(null, null);

        Field instanceField = MySQLManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    public void testAddGems_InteractionCount() {
        GemExecutor executor = new GemExecutor();
        String name = "testUser";
        int amount = 10;

        // Execute
        executor.addGems(name, amount);

        // Verify interactions for the optimized path
        verify(mysqlManagerMock, times(1)).addGems(eq(name), eq(amount));

        // Ensure other methods are NOT called
        verify(mysqlManagerMock, never()).getGems(anyString());
        verify(mysqlManagerMock, never()).getTotal(anyString());
        verify(mysqlManagerMock, never()).insertData(anyString());
        verify(mysqlManagerMock, never()).setGems(anyString(), anyInt());
        verify(mysqlManagerMock, never()).setTotal(anyString(), anyInt());
    }
}
