package com.mcatk.gem;

import com.mcatk.gem.sql.MySQLManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GemExecutorTest {

    @Mock
    private MySQLManager mySQLManagerMock;

    @Mock
    private Gem gemMock;

    private GemExecutor gemExecutor;

    @Before
    public void setUp() throws Exception {
        // Mock MySQLManager.instance
        setStaticField(MySQLManager.class, "instance", mySQLManagerMock);

        // Mock Gem.plugin
        setStaticField(Gem.class, "plugin", gemMock);

        gemExecutor = new GemExecutor();
    }

    @After
    public void tearDown() throws Exception {
        setStaticField(MySQLManager.class, "instance", null);
        setStaticField(Gem.class, "plugin", null);
    }

    private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Test
    public void testAddGems_Performance() {
        String playerName = "TestPlayer";
        int gemsToAdd = 100;

        // Simulate new user: getGems returns null
        when(mySQLManagerMock.getGems(playerName)).thenReturn(null);

        // Mock getTotal to return a value (e.g. 0)
        when(mySQLManagerMock.getTotal(playerName)).thenReturn(0);

        gemExecutor.addGems(playerName, gemsToAdd);

        // Verify getGems is called exactly ONCE (target behavior)
        verify(mySQLManagerMock, times(1)).getGems(playerName);

        // Verify insertData is called
        verify(mySQLManagerMock).insertData(playerName);

        // Verify update calls
        verify(mySQLManagerMock).setGems(eq(playerName), eq(gemsToAdd));
        verify(mySQLManagerMock).setTotal(eq(playerName), eq(gemsToAdd));

        // Verify log
        verify(gemMock).log(anyString());
    }
}
