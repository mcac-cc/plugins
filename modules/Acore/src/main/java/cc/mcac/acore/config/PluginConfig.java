package cc.mcac.acore.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class PluginConfig {
    @Setting("server-id")
    public String serverId = "velocity";

    @Setting("player-list-to-db-interval")
    public  int playerListToDbInterval = 60;

    @Setting("database")
    public Database database = new Database();

    @ConfigSerializable
    public static class Database {
        @Setting("jdbc-url")
        public String jdbcUrl = "jdbc:mysql://localhost:3306/";
        @Setting("username")
        public String username = "username";
        @Setting("password")
        public String password = "password";
    }
}
