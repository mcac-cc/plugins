package com.mcatk.gem;

import com.mcatk.gem.sql.MySQLManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
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
    public void addUsesOneAtomicUpsertWithoutReadModifyWrite() {
        new GemExecutor().addGems("TestPlayer", 100);
        verify(mySQLManager).addGems("TestPlayer", 100);
        verify(mySQLManager, never()).getData(anyString());
        verify(mySQLManager, never()).insertData(anyString());
        verify(mySQLManager, never()).updateData(anyString(), anyInt(), anyInt());
    }

    @Test
    public void refundDoesNotCountAsANewLifetimeAward() {
        when(mySQLManager.refundGems("TestPlayer", 20)).thenReturn(true);
        assertTrue(new GemExecutor().refundGems("TestPlayer", 20));
        verify(mySQLManager).refundGems("TestPlayer", 20);
        verify(mySQLManager, never()).addGems(anyString(), anyInt());
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
        assertTrue(result);
        verify(mySQLManager, times(1)).reduceGems(playerName, amount);

        // Verify old methods not called
        verify(mySQLManager, never()).getGems(playerName);
        verify(mySQLManager, never()).setGems(anyString(), anyInt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddGemsRejectsNonPositiveAmounts() {
        GemExecutor executor = new GemExecutor();

        executor.addGems("TestPlayer", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetGemsRejectsNonPositiveAmounts() {
        GemExecutor executor = new GemExecutor();

        executor.setGems("TestPlayer", -1);
    }

    @Test
    public void testTakeGemsRejectsNonPositiveAmounts() {
        GemExecutor executor = new GemExecutor();

        assertFalse(executor.takeGems("TestPlayer", 0));
        assertFalse(executor.takeGems("TestPlayer", -5));
        verify(mySQLManager, never()).reduceGems(anyString(), anyInt());
    }
    @Test
    public void databaseReadFailureDoesNotCreateOrOverwriteBalance() {
        doThrow(new IllegalStateException("Database unavailable")).when(mySQLManager).addGems("TestPlayer", 20);

        assertThrows(IllegalStateException.class, () -> new GemExecutor().addGems("TestPlayer", 20));

        verify(mySQLManager, never()).insertData(anyString());
        verify(mySQLManager, never()).updateData(anyString(), anyInt(), anyInt());
        verify(gem, never()).log(anyString());
    }
}
