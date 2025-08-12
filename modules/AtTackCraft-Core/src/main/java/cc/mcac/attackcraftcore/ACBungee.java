package cc.mcac.attackcraftcore;

import cc.mcac.attackcraftcore.Bungee.Announce;
import cc.mcac.attackcraftcore.Bungee.Broadcast;
import cc.mcac.attackcraftcore.Bungee.PlayerListLogger;
import cc.mcac.attackcraftcore.Bungee.WhiteList.WhiteList;
import cc.mcac.attackcraftcore.Bungee.WhiteList.WhitelistCommand;
import cc.mcac.attackcraftcore.Bungee.miraiMC;
import cc.mcac.attackcraftcore.SQL.SQLManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;

public class ACBungee extends Plugin {
    public static ACBungee plugin = null;
    private Configuration configuration;
    private SQLManager sqlManager;

    private WhiteList whiteList;

    @Override
    public void onEnable() {
        plugin = this;
        loadConfig();
        setupSQL();
        loadWhitelist();
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Announce());
        if (getProxy().getPluginManager().getPlugin("MiraiMC") != null) {
            getProxy().getPluginManager().registerListener(this, new miraiMC(this, whiteList));
            getLogger().info("接入 miraiMC");
        }
        if (getConfiguration().getBoolean("playerlistlogger")) {
            new PlayerListLogger(this).run();
        }
        setUpBStats();
        new Broadcast(this).run();
        getLogger().info("AtTackCraft-Core 已启动");
        // Plugin startup logic
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadConfig() {
        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File file = new File(getDataFolder(), "config.yml");

            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).
                    load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private void setupSQL() {
        try {
            sqlManager = new SQLManager(
                    getConfiguration().getString("mysql.host"),
                    getConfiguration().getString("mysql.database"),
                    getConfiguration().getString("mysql.username"),
                    getConfiguration().getString("mysql.password")
            );
            getLogger().info("数据库连接成功");
        } catch (SQLException e) {
            getLogger().warning("数据库连接失败");
            e.printStackTrace();
        }
    }

    public SQLManager getSqlManager() {
        return sqlManager;
    }

    private void loadWhitelist() {
        whiteList = new WhiteList(this);
        getProxy().getPluginManager().registerListener(this, whiteList);
        getProxy().getPluginManager().registerCommand(this, new WhitelistCommand(this, whiteList));
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpBStats() {
        int pluginId = 17320;
        Metrics metrics = new Metrics(this, pluginId);
    }
}
