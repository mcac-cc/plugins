package cc.mcac.acore.database;

import cc.mcac.acore.Acore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final Acore acore;
    private final HikariDataSource dataSource;

    public DatabaseManager(Acore acore) {
        this.acore = acore;
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(acore.getPluginConfig().database.jdbcUrl);
        config.setUsername(acore.getPluginConfig().database.username);
        config.setPassword(acore.getPluginConfig().database.password);

        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        this.dataSource = new HikariDataSource(config);
        acore.getLogger().info("Database connection established");
    }

    public void updatePlayerList(String serverId, int playerCount, String playerList) {
        String sql = "INSERT INTO `server_player_list` (server_id, player_count, player_list) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE player_count = ?, player_list = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            statement.setInt(2, playerCount);
            statement.setString(3, playerList);
            statement.setInt(4, playerCount);
            statement.setString(5, playerList);
            statement.executeUpdate();
        } catch (SQLException e) {
            acore.getLogger().error("Failed to update player list to database", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

}
