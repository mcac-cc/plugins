package cc.mcac.attackcraftcore.SQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLManager {

    private Connection connection;

    public SQLManager(String host, String database, String username, String password) throws SQLException {
        int port = 3306;
        connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false",
                username, password
        );
    }

    public Connection getConnection() {
        return connection;
    }

}
