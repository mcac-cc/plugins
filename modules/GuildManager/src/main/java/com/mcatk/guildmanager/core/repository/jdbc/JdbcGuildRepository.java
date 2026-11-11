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
    public synchronized boolean removeMember(String playerId, String guildId) {
        String sql = "DELETE FROM player_guild WHERE player_id=? AND guild_id=? " +
                "AND NOT EXISTS (SELECT 1 FROM guild WHERE guild_id=? " +
                "AND (? = guild_chairman OR ? = guild_vice_chairman_1 OR ? = guild_vice_chairman_2))";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId);
            ps.setString(2, guildId);
            ps.setString(3, guildId);
            for (int i = 4; i <= 6; i++) ps.setString(i, playerId);
            return ps.executeUpdate() == 1;
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

    @Override
    public synchronized void validateTransactionalTables() {
        try (PreparedStatement statement = getConnection().prepareStatement(
                "SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('guild','player_guild')");
             ResultSet result = statement.executeQuery()) {
            int tables = 0;
            while (result.next()) {
                if (!"InnoDB".equalsIgnoreCase(result.getString("ENGINE"))) {
                    throw new IllegalStateException("Guild payment tables must use InnoDB");
                }
                tables++;
            }
            if (tables != 2) throw new IllegalStateException("Guild payment tables are missing");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot validate Guild payment tables", e);
        }
    }

    @FunctionalInterface
    private interface TransactionWork { void run(Connection connection) throws SQLException; }

    private void transaction(TransactionWork work) {
        boolean committing = false;
        boolean committed = false;
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            work.run(connection);
            committing = true;
            connection.commit();
            committed = true;
        } catch (SQLException | RuntimeException e) {
            if (connection != null && !committing) {
                try { connection.rollback(); }
                catch (SQLException rollback) { e.addSuppressed(rollback); }
            }
            closeConnection();
            throw new GuildTransactionException("Guild transaction failed", e, committing);
        } finally {
            if (committed) {
                try { connection.setAutoCommit(true); }
                catch (SQLException e) { closeConnection(); } // The commit already succeeded; do not refund it.
            }
        }
    }

    @Override
    public synchronized void createGuildWithChairman(String id, String chairman) {
        transaction(connection -> {
            try (PreparedStatement guild = connection.prepareStatement(SqlCommand.CREATE_GUILD.toString());
                 PreparedStatement member = connection.prepareStatement(SqlCommand.CREATE_PLAYER.toString())) {
                guild.setString(1, id); guild.setString(2, chairman); guild.executeUpdate();
                member.setString(1, chairman); member.setString(2, id); member.executeUpdate();
            }
        });
    }

    @Override
    public synchronized void donate(String playerId, String guildId, int cash, int contribution) {
        if (cash <= 0 || contribution <= 0) throw new IllegalArgumentException("Donation must be positive");
        transaction(connection -> {
            try (PreparedStatement member = connection.prepareStatement(
                    "SELECT player_id FROM player_guild WHERE player_id=? AND guild_id=? FOR UPDATE")) {
                member.setString(1, playerId); member.setString(2, guildId);
                try (ResultSet result = member.executeQuery()) {
                    if (!result.next()) throw new SQLException("Player is no longer a guild member");
                }
            }
            try (PreparedStatement guild = connection.prepareStatement(
                    "UPDATE guild SET guild_cash=guild_cash+? WHERE guild_id=?");
                 PreparedStatement member = connection.prepareStatement(
                    "UPDATE player_guild SET player_contribution=LEAST(100,player_contribution+?) WHERE player_id=? AND guild_id=?")) {
                guild.setInt(1, cash); guild.setString(2, guildId);
                if (guild.executeUpdate() != 1) throw new SQLException("Guild no longer exists");
                member.setInt(1, contribution); member.setString(2, playerId); member.setString(3, guildId);
                member.executeUpdate();
            }
        });
    }
}
