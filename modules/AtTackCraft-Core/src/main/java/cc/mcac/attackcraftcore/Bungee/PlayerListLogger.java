package cc.mcac.attackcraftcore.Bungee;

import cc.mcac.attackcraftcore.ACBungee;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class PlayerListLogger {

    private ACBungee plugin;

    public PlayerListLogger(ACBungee plugin) {
        this.plugin = plugin;
    }

    public void run() {
        plugin.getLogger().info("PlayerListLogger 开始工作");
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            try {
                Connection connection = plugin.getSqlManager().getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO `server_player_list` (server_id, player_number, player_list) VALUES (?,?,?) " +
                                "ON DUPLICATE KEY UPDATE player_number = ?, player_list = ?"
                );
                int playerNumber = plugin.getProxy().getOnlineCount();
                String playerList = plugin.getProxy().getPlayers().toString();
                ps.setString(1, plugin.getConfiguration().getString("server_id"));
                ps.setInt(2, playerNumber);
                ps.setString(3, playerList);
                ps.setInt(4, playerNumber);
                ps.setString(5, playerList);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

}
