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
    public void testAddGemsRedundantCall() {
        GemExecutor executor = new GemExecutor();
        String playerName = "TestPlayer";
        int amount = 100;
        int currentGems = 50;
        int currentTotal = 1000;

        // Setup mock behavior
        when(mySQLManager.getGems(playerName)).thenReturn(currentGems);
        when(mySQLManager.getTotal(playerName)).thenReturn(currentTotal);

        // Execute
        executor.addGems(playerName, amount);

        // Verify
        // Expecting 1 call (the optimized behavior)
        // This will FAIL on unoptimized code (it calls 2 times)
        verify(mySQLManager, times(1)).getGems(playerName);

        verify(mySQLManager, times(1)).getTotal(playerName);
        verify(mySQLManager, times(1)).setGems(playerName, currentGems + amount);
        verify(mySQLManager, times(1)).setTotal(playerName, currentTotal + amount);
    }
}
