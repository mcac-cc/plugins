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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GemExecutorTest {

    @Mock
    private MySQLManager mySQLManager;

    @Mock
    private Gem gemPlugin;

    @Mock
    private Logger logger;

    private GemExecutor gemExecutor;

    @Before
    public void setUp() throws Exception {
        // Mock Gem.plugin
        setStaticField(Gem.class, "plugin", gemPlugin);
        // when(gemPlugin.getLogger()).thenReturn(logger);

        // Mock MySQLManager.instance
        setStaticField(MySQLManager.class, "instance", mySQLManager);

        gemExecutor = new GemExecutor();
    }

    @After
    public void tearDown() throws Exception {
        setStaticField(Gem.class, "plugin", null);
        setStaticField(MySQLManager.class, "instance", null);
    }

    private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Test
    public void testAddGems_Optimized() {
        String playerName = "TestPlayer";
        int gemsToAdd = 100;

        gemExecutor.addGems(playerName, gemsToAdd);

        // Verify that addGems is called exactly once
        verify(mySQLManager).addGems(playerName, gemsToAdd);

        // Verify that redundant calls are NOT made
        verify(mySQLManager, never()).getGems(anyString());
        verify(mySQLManager, never()).getTotal(anyString());
        verify(mySQLManager, never()).setGems(anyString(), anyInt());
        verify(mySQLManager, never()).setTotal(anyString(), anyInt());
        verify(mySQLManager, never()).insertData(anyString());
    }
}
