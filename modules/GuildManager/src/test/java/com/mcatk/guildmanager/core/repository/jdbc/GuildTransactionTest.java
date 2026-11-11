package com.mcatk.guildmanager.core.repository.jdbc;

import com.mcatk.guildmanager.core.config.DbConfig;
import org.junit.Before;
import org.junit.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.Assert.*;

public class GuildTransactionTest {
    private String url;
    private JdbcGuildRepository repository;
    @Before public void setUp() throws Exception {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        repository=new JdbcGuildRepository(new DbConfig("unused",3306,"test","sa","") {
            @Override public String toJdbcUrl() { return url; }
        });
        try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement()) {
            s.execute("CREATE TABLE guild(guild_id VARCHAR(32) PRIMARY KEY,guild_chairman VARCHAR(64),guild_cash INT DEFAULT 0)");
            s.execute("CREATE TABLE player_guild(player_id VARCHAR(64) PRIMARY KEY,guild_id VARCHAR(32),player_contribution INT DEFAULT 0,player_is_advanced BOOLEAN DEFAULT FALSE,FOREIGN KEY(guild_id) REFERENCES guild(guild_id))");
        }
    }
    private int count(String query) throws Exception {
        try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement();ResultSet r=s.executeQuery(query)) {r.next();return r.getInt(1);}
    }
    @Test public void creationCommitsGuildAndChairmanTogether() throws Exception {
        repository.createGuildWithChairman("ac","Alice");
        assertEquals(1,count("SELECT COUNT(*) FROM guild WHERE guild_id='ac'"));
        assertEquals(1,count("SELECT COUNT(*) FROM player_guild WHERE player_id='Alice' AND guild_id='ac'"));
    }
    @Test public void memberInsertFailureRollsBackTheNewGuild() throws Exception {
        repository.createGuildWithChairman("existing","Alice");
        GuildTransactionException error=assertThrows(GuildTransactionException.class,()->repository.createGuildWithChairman("newguild","Alice"));
        assertFalse(error.isOutcomeUnknown());
        assertEquals(0,count("SELECT COUNT(*) FROM guild WHERE guild_id='newguild'"));
        assertEquals(1,count("SELECT COUNT(*) FROM player_guild WHERE player_id='Alice' AND guild_id='existing'"));
    }
    @Test public void duplicateGuildIdDoesNotAddAnotherChairman() throws Exception {
        repository.createGuildWithChairman("ac","Alice");
        assertThrows(GuildTransactionException.class,()->repository.createGuildWithChairman("ac","Bob"));
        assertEquals(0,count("SELECT COUNT(*) FROM player_guild WHERE player_id='Bob'"));
    }
    @Test public void donationUpdatesBothBalancesAndCapsContribution() throws Exception {
        repository.createGuildWithChairman("ac","Alice");
        repository.donate("Alice","ac",120,120);
        assertEquals(120,count("SELECT guild_cash FROM guild WHERE guild_id='ac'"));
        assertEquals(100,count("SELECT player_contribution FROM player_guild WHERE player_id='Alice'"));
    }
    @Test public void failedContributionUpdateRollsBackGuildFunds() throws Exception {
        repository.createGuildWithChairman("ac","Alice");
        try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement()) {
            s.execute("ALTER TABLE player_guild ADD CONSTRAINT no_contribution CHECK(player_contribution=0)");
        }
        assertThrows(GuildTransactionException.class,()->repository.donate("Alice","ac",1,1));
        assertEquals(0,count("SELECT guild_cash FROM guild WHERE guild_id='ac'"));
    }
}
