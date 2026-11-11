package com.mcatk.gem.sql;

import com.mcatk.gem.Gem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLManager {
    private String ip;
    private String databaseName;
    private String userName;
    private String userPassword;
    private Connection connection;
    private int port;
    private boolean enabled;
    private volatile boolean ready;
    private static final MySQLManager INSTANCE = new MySQLManager();

    public static MySQLManager getInstance() {
        return INSTANCE;
    }

    public synchronized void enableMySQL() {
        ip = Gem.getPlugin().getConfig().getString("mysql.ip");
        databaseName = Gem.getPlugin().getConfig().getString("mysql.databasename");
        userName = Gem.getPlugin().getConfig().getString("mysql.username");
        userPassword = Gem.getPlugin().getConfig().getString("mysql.password");
        port = Gem.getPlugin().getConfig().getInt("mysql.port");
        enabled = true;
        try {
            getConnection();
            ready = true;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to the Gem database", e);
        }
    }

    public boolean isReady() { return ready; }

    public synchronized void shutdown() {
        ready = false;
        enabled = false;
        closeConnection();
    }

    private void closeConnection() {
        Connection previous = connection;
        connection = null;
        if (previous != null) {
            try {
                previous.close();
            } catch (SQLException e) {
                // A broken connection can fail to close; it must still be discarded.
            }
        }
    }

    // Callers hold this manager's monitor until their statement has finished.
    private Connection getConnection() throws SQLException {
        if (!enabled) {
            throw new SQLException("Gem database is not enabled");
        }
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) {
                    return connection;
                }
            } catch (SQLException e) {
                // Treat a failed validation as a stale connection.
            }
            closeConnection();
        }
        connection = DriverManager.getConnection(
                "jdbc:mysql://" + ip + ":" + port + "/" + databaseName
                        + "?useSSL=false&connectTimeout=5000&socketTimeout=5000",
                userName, userPassword
        );
        return connection;
    }

    public synchronized void insertData(String name) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO `gem` " +
                        "(`username`)" +
                        "VALUES (?)"
        )) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized Integer getGems(String name) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("gems");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
        return null;
    }

    public synchronized Integer getTotal(String name) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
        return null;
    }

    public synchronized void setGems(String name, int num) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE `gem` SET `gems` = ? WHERE `username` = ?"
        )) {
            ps.setString(2, name);
            ps.setInt(1, num);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized void setTotal(String name, int num) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE `gem` SET `total` = ? WHERE `username` = ?"
        )) {
            ps.setString(2, name);
            ps.setInt(1, num);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized boolean reduceGems(String name, int amount) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE `gem` SET `gems` = `gems` - ? WHERE `username` = ? AND `gems` >= ?"
        )) {
            ps.setInt(1, amount);
            ps.setString(2, name);
            ps.setInt(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized int[] getData(String name) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT `gems`, `total` FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("gems"), rs.getInt("total")};
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
        return null;
    }

    public synchronized void updateData(String name, int gems, int total) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE `gem` SET `gems` = ?, `total` = ? WHERE `username` = ?"
        )) {
            ps.setInt(1, gems);
            ps.setInt(2, total);
            ps.setString(3, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized boolean deleteData(String name) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized void addGems(String name, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("gems must be positive");
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO gem (username,gems,total) VALUES (?,?,?) "
                        + "ON DUPLICATE KEY UPDATE gems=COALESCE(gems,0)+VALUES(gems), total=COALESCE(total,0)+VALUES(total)")) {
            ps.setString(1, name);
            ps.setInt(2, amount);
            ps.setInt(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized void upsertGems(String name, int amount) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO gem (username,gems) VALUES (?,?) ON DUPLICATE KEY UPDATE gems=VALUES(gems)")) {
            ps.setString(1, name);
            ps.setInt(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gem database operation failed", e);
        }
    }

    public synchronized boolean refundGems(String name, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("gems must be positive");
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE gem SET gems=COALESCE(gems,0)+? WHERE username=?")) {
            ps.setInt(1, amount);
            ps.setString(2, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Gem refund failed; reconcile before retrying", e);
        }
    }
}
