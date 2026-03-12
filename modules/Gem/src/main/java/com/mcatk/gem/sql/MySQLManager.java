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
    private static MySQLManager instance = null;

    public static MySQLManager getInstance() {
        return instance == null ? instance = new MySQLManager() : instance;
    }

    public void enableMySQL() {
        ip = Gem.getPlugin().getConfig().getString("mysql.ip");
        databaseName = Gem.getPlugin().getConfig().getString("mysql.databasename");
        userName = Gem.getPlugin().getConfig().getString("mysql.username");
        userPassword = Gem.getPlugin().getConfig().getString("mysql.password");
        port = Gem.getPlugin().getConfig().getInt("mysql.port");
        connectMySQL();
    }

    public void shutdown() {
        try {
            connection.close();
        } catch (SQLException e) {
            //断开连接失败
            e.printStackTrace();
        }
    }

    private void connectMySQL() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + ip + ":" + port + "/" + databaseName + "?autoReconnect=true&useSSL=false",
                    userName, userPassword
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertData(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO `gem` " +
                        "(`username`)" +
                        "VALUES (?)"
        )) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Integer getGems(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("gems");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer getTotal(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setGems(String name, int num) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE `gem` SET `gems` = ? WHERE `username` = ?"
        )) {
            ps.setString(2, name);
            ps.setInt(1, num);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTotal(String name, int num) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE `gem` SET `total` = ? WHERE `username` = ?"
        )) {
            ps.setString(2, name);
            ps.setInt(1, num);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean reduceGems(String name, int amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE `gem` SET `gems` = `gems` - ? WHERE `username` = ? AND `gems` >= ?"
        )) {
            ps.setInt(1, amount);
            ps.setString(2, name);
            ps.setInt(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int[] getData(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT `gems`, `total` FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new int[]{rs.getInt("gems"), rs.getInt("total")};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateData(String name, int gems, int total) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE `gem` SET `gems` = ?, `total` = ? WHERE `username` = ?"
        )) {
            ps.setInt(1, gems);
            ps.setInt(2, total);
            ps.setString(3, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteData(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM `gem` WHERE `username` = ?"
        )) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
