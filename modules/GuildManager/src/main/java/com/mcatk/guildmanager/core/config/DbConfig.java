package com.mcatk.guildmanager.core.config;

public class DbConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public DbConfig(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String toJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
    }
}
