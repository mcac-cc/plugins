package com.mcatk.gem;

import com.mcatk.gem.sql.MySQLManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class GemExecutorTest {

    private MockedStatic<MySQLManager> mockedMySQLManager;
    private MySQLManager mySQLManager;
    private MockedStatic<Gem> mockedGem;
    private Gem gem;

    @Before
    public void setUp() {
        // Mock MySQLManager static getInstance
        mockedMySQLManager = mockStatic(MySQLManager.class);
        mySQLManager = mock(MySQLManager.class);
        mockedMySQLManager.when(MySQLManager::getInstance).thenReturn(mySQLManager);

        // Mock Gem static getPlugin
        mockedGem = mockStatic(Gem.class);
        gem = mock(Gem.class);
        mockedGem.when(Gem::getPlugin).thenReturn(gem);
    }

    @After
    public void tearDown() {
        if (mockedMySQLManager != null) {
            mockedMySQLManager.close();
        }
        if (mockedGem != null) {
            mockedGem.close();
        }
    }

    @Test
    public void testAddGemsOptimized() {
        GemExecutor executor = new GemExecutor();
        String playerName = "TestPlayer";
        int amount = 100;
        int currentGems = 50;
        int currentTotal = 1000;

        // Setup mock behavior for the new getData method
        when(mySQLManager.getData(playerName)).thenReturn(new int[]{currentGems, currentTotal});

        // Execute
        executor.addGems(playerName, amount);

        // Verify that the new efficient methods are called
        verify(mySQLManager, times(1)).getData(playerName);
        verify(mySQLManager, times(1)).updateData(playerName, currentGems + amount, currentTotal + amount);

        // Verify that the old inefficient methods are NOT called
        verify(mySQLManager, never()).getGems(playerName);
        verify(mySQLManager, never()).getTotal(playerName);
        verify(mySQLManager, never()).setGems(anyString(), anyInt());
        verify(mySQLManager, never()).setTotal(anyString(), anyInt());
    }

    @Test
    public void testTakeGemsOptimized() {
        GemExecutor executor = new GemExecutor();
        String playerName = "TestPlayer";
        int amount = 10;

        // Setup mock behavior for reduceGems
        when(mySQLManager.reduceGems(playerName, amount)).thenReturn(true);

        // Execute
        boolean result = executor.takeGems(playerName, amount);

        // Verify
        assert(result);
        verify(mySQLManager, times(1)).reduceGems(playerName, amount);

        // Verify old methods not called
        verify(mySQLManager, never()).getGems(playerName);
        verify(mySQLManager, never()).setGems(anyString(), anyInt());
    }
}
