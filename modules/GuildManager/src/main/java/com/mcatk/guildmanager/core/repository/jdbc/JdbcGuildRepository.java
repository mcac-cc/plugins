package com.mcatk.guildmanager.core.repository.jdbc;

import com.mcatk.guildmanager.core.config.DbConfig;
import com.mcatk.guildmanager.core.repository.GuildRepository;
import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.Member;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class JdbcGuildRepository implements GuildRepository {
    private final DbConfig config;
    private Connection connection;

    public JdbcGuildRepository(DbConfig config) {
        this.config = config;
        connect();
    }

    private synchronized void connect() {
        try {
            connection = DriverManager.getConnection(
                    config.toJdbcUrl(),
                    config.getUsername(),
                    config.getPassword()
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    @Override
    public void createGuild(String id, String chairman) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.CREATE_GUILD.toString())) {
            ps.setString(1, id);
            ps.setString(2, chairman);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateGuild(Guild g) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.UPDATE_GUILD.toString())) {
            ps.setString(1, g.getGuildName());
            ps.setString(2, g.getChairman());
            ps.setString(3, g.getViceChairman1());
            ps.setString(4, g.getViceChairman2());
            ps.setInt(5, g.getLevel());
            ps.setInt(6, g.getPoints());
            ps.setInt(7, g.getCash());
            ps.setBoolean(8, g.getResidenceFLag());
            ps.setBoolean(9, g.getHasChangedName());
            ps.setString(10, g.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPlayerGuildId(String playerId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_PLAYER_GUILD.toString())) {
            ps.setString(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("g.guild_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Guild getGuild(String guildId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_GUILD.toString())) {
            ps.setString(1, guildId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Guild g = new Guild();
                g.setId(rs.getString("guild_id"));
                g.setGuildName(rs.getString("guild_name"));
                g.setChairman(rs.getString("guild_chairman"));
                g.setViceChairman1(rs.getString("guild_vice_chairman_1"));
                g.setViceChairman2(rs.getString("guild_vice_chairman_2"));
                g.setLevel(rs.getInt("guild_level"));
                g.setPoints(rs.getInt("guild_points"));
                g.setCash(rs.getInt("guild_cash"));
                g.setResidenceFLag(rs.getBoolean("guild_has_residence"));
                g.setHasChangedName(rs.getBoolean("guild_has_changed_name"));
                g.setMembers(getMembersFromSQL(g.getId()));
                return g;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Member getMember(String id) {
        Member m = null;
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_PLAYER.toString())) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                m = new Member(
                        rs.getString("player_id"),
                        rs.getString("guild_id"),
                        rs.getInt("player_contribution"),
                        rs.getBoolean("player_is_advanced")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    @Override
    public void updateMember(Member m) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.UPDATE_PLAYER.toString())) {
            ps.setBoolean(1, m.isAdvanced());
            ps.setInt(2, m.getContribution());
            ps.setString(3, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addMember(String playerId, String guildId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.CREATE_PLAYER.toString())) {
            ps.setString(1, playerId);
            ps.setString(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeMember(String playerId, String guildId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.DELETE_PLAYER.toString())) {
            ps.setString(1, playerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public HashMap<String, Guild> getAllGuilds() {
        HashMap<String, Guild> guilds = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_ALL_GUILDS.toString())) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Guild g = new Guild();
                g.setId(rs.getString("guild_id"));
                g.setGuildName(rs.getString("guild_name"));
                g.setChairman(rs.getString("guild_chairman"));
                g.setViceChairman1(rs.getString("guild_vice_chairman_1"));
                g.setViceChairman2(rs.getString("guild_vice_chairman_2"));
                g.setLevel(rs.getInt("guild_level"));
                g.setPoints(rs.getInt("guild_points"));
                g.setCash(rs.getInt("guild_cash"));
                g.setResidenceFLag(rs.getBoolean("guild_has_residence"));
                g.setHasChangedName(rs.getBoolean("guild_has_changed_name"));
                guilds.put(g.getId(), g);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_ALL_MEMBERS.toString())) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Member m = new Member(
                        rs.getString("player_id"),
                        rs.getString("guild_id"),
                        rs.getInt("player_contribution"),
                        rs.getBoolean("player_is_advanced")
                );
                Guild g = guilds.get(m.getGuildID());
                if (g != null) {
                    g.getMembers().add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return guilds;
    }

    private ArrayList<Member> getMembersFromSQL(String guildId) {
        ArrayList<Member> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM `player_guild` WHERE guild_id = ?")) {
            ps.setString(1, guildId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Member m = new Member(
                        rs.getString("player_id"),
                        rs.getString("guild_id"),
                        rs.getInt("player_contribution"),
                        rs.getBoolean("player_is_advanced")
                );
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
