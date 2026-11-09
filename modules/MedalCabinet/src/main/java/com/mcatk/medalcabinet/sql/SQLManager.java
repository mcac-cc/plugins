package com.mcatk.medalcabinet.sql;

import com.mcatk.medalcabinet.MedalCabinet;
import com.mcatk.medalcabinet.medal.Medal;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SQLManager {
    private Connection connection;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private boolean stopped;

    private static SQLManager instance = null;
    private final Map<String, Medal> mainMedalCache = new ConcurrentHashMap<>();
    private static final Medal EMPTY_MEDAL = new Medal("", "", "", "");

    public static synchronized SQLManager getInstance() {
        return instance == null ? instance = new SQLManager() : instance;
    }

    public static synchronized void setInstance(SQLManager instance) {
        SQLManager.instance = instance;
    }

    private SQLManager() {
        this("jdbc:mysql://" + MedalCabinet.getPlugin().getConfig().getString("mysql.ip")
                        + ":" + MedalCabinet.getPlugin().getConfig().getInt("mysql.port")
                        + "/" + MedalCabinet.getPlugin().getConfig().getString("mysql.databasename")
                        + "?useSSL=false&connectTimeout=5000&socketTimeout=5000",
                MedalCabinet.getPlugin().getConfig().getString("mysql.username"),
                MedalCabinet.getPlugin().getConfig().getString("mysql.password"));
    }

    SQLManager(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public static synchronized void shutdownInstance() {
        if (instance != null) {
            instance.shutdown();
        }
    }

    public synchronized void shutdown() {
        stopped = true;
        closeConnection();
        mainMedalCache.clear();
    }

    private void closeConnection() {
        Connection previous = connection;
        connection = null;
        if (previous != null) {
            try {
                previous.close();
            } catch (SQLException ignored) {
                // Discard broken connections even if closing them fails.
            }
        }
    }

    // Public operations hold this manager's monitor until their statements finish.
    private Connection getConnection() throws SQLException {
        if (stopped) {
            throw new SQLException("MedalCabinet database is shut down");
        }
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) {
                    return connection;
                }
            } catch (SQLException ignored) {
                // A failed validation also requires replacing the connection.
            }
            closeConnection();
        }
        connection = DriverManager.getConnection(jdbcUrl, username, password);
        return connection;
    }

    public synchronized void createMedal(String medalID, String medalName, String medalMat, String medalDes) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO `medal` " +
                        "(`medal_id`, `medal_name`, `medal_material`,`medal_description`)" +
                        "VALUES " +
                        "(?,?,?,?)"
        )) {
            ps.setString(1, medalID);
            ps.setString(2, medalName);
            ps.setString(3, medalMat);
            ps.setString(4, medalDes);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean doesPlayerHaveMedal(String playerID, String medalID) {
        try {
            return hasPlayerMedal(playerID, medalID);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean hasPlayerMedal(String playerID, String medalID) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM player_medal WHERE player_id = ? and medal_id = ?"
        )) {
            ps.setString(1, playerID);
            ps.setString(2, medalID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public synchronized void addPlayerMedal(String playerID, String medalID) {
        try {
            if (hasPlayerMedal(playerID, medalID)) {
                return;
            }
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO `player_medal` " +
                            "(`player_id`, `medal_id`)" +
                            "VALUES " +
                            "(?,?)"
            )) {
                ps.setString(1, playerID);
                ps.setString(2, medalID);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized ArrayList<Medal> getAllMedals() {
        ArrayList<Medal> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM medal"
        )) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Medal(
                            rs.getString("medal_id"),
                            rs.getString("medal_name"),
                            rs.getString("medal_material"),
                            rs.getString("medal_description")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized Medal getMedal(String medalID) {
        Medal medal = null;
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM medal WHERE medal_id = ?"
        )) {
            ps.setString(1, medalID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    medal = new Medal(
                            rs.getString("medal_id"),
                            rs.getString("medal_name"),
                            rs.getString("medal_material"),
                            rs.getString("medal_description")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return medal;
    }

    public synchronized ArrayList<Medal> getPlayerMedals(String playerID) {
        ArrayList<Medal> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT DISTINCT m.* FROM medal m INNER JOIN player_medal pm ON m.medal_id = pm.medal_id WHERE pm.player_id = ?"
        )) {
            ps.setString(1, playerID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Medal medal = new Medal(
                            rs.getString("medal_id"),
                            rs.getString("medal_name"),
                            rs.getString("medal_material"),
                            rs.getString("medal_description")
                    );
                    list.add(medal);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized boolean setMainMedal(String playerID, String medalID) {
        try {
            if (!hasPlayerMedal(playerID, medalID)) {
                return false;
            }
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO `player_main_medal` (player_id, medal_id) VALUES (?,?) ON DUPLICATE KEY UPDATE medal_id = ?"
            )) {
                ps.setString(1, playerID);
                ps.setString(2, medalID);
                ps.setString(3, medalID);
                // The update may commit even if its response is lost.
                mainMedalCache.remove(playerID);
                ps.executeUpdate();

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized Medal getMainMedal(String playerID) {
        Medal cached = mainMedalCache.get(playerID);
        if (cached != null) {
            return cached == EMPTY_MEDAL ? null : cached;
        }

        Medal medal = null;
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT m.* FROM medal m INNER JOIN player_main_medal pm "
                        + "ON m.medal_id = pm.medal_id WHERE pm.player_id = ?"
        )) {
            ps.setString(1, playerID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    medal = new Medal(rs.getString("medal_id"), rs.getString("medal_name"),
                            rs.getString("medal_material"), rs.getString("medal_description"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // An unavailable database does not mean the player has no medal.
            return null;
        }
        mainMedalCache.put(playerID, medal == null ? EMPTY_MEDAL : medal);
        return medal;
    }

    public synchronized void clearCache(String playerID) {
        mainMedalCache.remove(playerID);
    }

}
