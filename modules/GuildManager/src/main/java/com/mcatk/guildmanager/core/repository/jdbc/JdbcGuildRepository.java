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

public class JdbcGuildRepository implements GuildRepository, AutoCloseable {
    private final DbConfig config;
    private Connection connection;
    private boolean stopped;

    public JdbcGuildRepository(DbConfig config) {
        this.config = config;
        try {
            getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to the GuildManager database", e);
        }
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

    @Override
    public synchronized void close() {
        stopped = true;
        closeConnection();
    }

    // Repository operations keep the monitor until all their statements finish.
    private Connection getConnection() throws SQLException {
        if (stopped) {
            throw new SQLException("GuildManager database is shut down");
        }
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) {
                    return connection;
                }
            } catch (SQLException ignored) {
                // Failed validation is equivalent to an expired connection.
            }
            closeConnection();
        }
        connection = DriverManager.getConnection(config.toJdbcUrl(), config.getUsername(), config.getPassword());
        return connection;
    }

    @Override
    public synchronized void createGuild(String id, String chairman) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.CREATE_GUILD.toString())) {
            ps.setString(1, id);
            ps.setString(2, chairman);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
    }

    @Override
    public synchronized void updateGuild(Guild g) {
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
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
    }

    @Override
    public synchronized String getPlayerGuildId(String playerId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_PLAYER_GUILD.toString())) {
            ps.setString(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("g.guild_id");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
        return null;
    }

    @Override
    public synchronized Guild getGuild(String guildId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_GUILD.toString())) {
            ps.setString(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
        return null;
    }

    @Override
    public synchronized Member getMember(String id) {
        Member m = null;
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_PLAYER.toString())) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m = new Member(
                            rs.getString("player_id"),
                            rs.getString("guild_id"),
                            rs.getInt("player_contribution"),
                            rs.getBoolean("player_is_advanced")
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
        return m;
    }

    @Override
    public synchronized void updateMember(Member m) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.UPDATE_PLAYER.toString())) {
            ps.setBoolean(1, m.isAdvanced());
            ps.setInt(2, m.getContribution());
            ps.setString(3, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
    }

    @Override
    public synchronized void addMember(String playerId, String guildId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.CREATE_PLAYER.toString())) {
            ps.setString(1, playerId);
            ps.setString(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
    }

    @Override
    public synchronized void removeMember(String playerId, String guildId) {
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.DELETE_PLAYER.toString())) {
            ps.setString(1, playerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
    }

    @Override
    public synchronized HashMap<String, Guild> getAllGuilds() {
        HashMap<String, Guild> guilds = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(SqlCommand.GET_ALL_GUILDS.toString())) {
            try (ResultSet rs = ps.executeQuery()) {
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
                    g.setMembers(new ArrayList<>());
                    guilds.put(g.getId(), g);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }

        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM `player_guild`")) {
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
        return guilds;
    }

    private ArrayList<Member> getMembersFromSQL(String guildId) {
        ArrayList<Member> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM `player_guild` WHERE guild_id = ?")) {
            ps.setString(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Member m = new Member(
                            rs.getString("player_id"),
                            rs.getString("guild_id"),
                            rs.getInt("player_contribution"),
                            rs.getBoolean("player_is_advanced")
                    );
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("GuildManager database operation failed", e);
        }
        return list;
    }
}
