package cc.mcac.attackcraftcore.SQL;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SQLManagerTest {
    @Test
    public void callerOwnsAFreshConnectionForEachOperation() throws Exception {
        Connection startup = mock(Connection.class);
        Connection first = mock(Connection.class);
        Connection second = mock(Connection.class);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(anyString(), eq("test"), eq("test")))
                    .thenReturn(startup, first, second);
            SQLManager manager = new SQLManager("localhost", "test", "test", "test");
            verify(startup).close();
            try (Connection connection = manager.getConnection()) { assertSame(first, connection); }
            try (Connection connection = manager.getConnection()) { assertSame(second, connection); }
            verify(first).close();
            verify(second).close();
        }
    }

    @Test
    public void failedOpenIsReportedAndTheNextOperationCanRecover() throws Exception {
        SQLException failure1 = new SQLException("Unavailable");
        Connection startup = mock(Connection.class);
        Connection recovered = mock(Connection.class);
        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(anyString(), eq("test"), eq("test")))
                    .thenReturn(startup).thenThrow(failure1).thenReturn(recovered);
            SQLManager manager = new SQLManager("localhost", "test", "test", "test");
            assertThrows(SQLException.class, manager::getConnection);
            try (Connection connection = manager.getConnection()) { assertSame(recovered, connection); }
            verify(recovered).close();
        }
    }
}
