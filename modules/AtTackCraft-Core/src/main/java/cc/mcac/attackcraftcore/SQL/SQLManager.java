package cc.mcac.attackcraftcore.SQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLManager {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public SQLManager(String host, String database, String username, String password) throws SQLException {
        jdbcUrl = "jdbc:mysql://" + host + ":3306/" + database
                + "?useSSL=false&connectTimeout=5000&socketTimeout=5000";
        this.username = username;
        this.password = password;
        // Preserve startup validation without retaining an idle connection.
        try (Connection ignored = getConnection()) {
        }
    }

    // Each caller owns and closes its connection; scheduled jobs cannot share it.
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
