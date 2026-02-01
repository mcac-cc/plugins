package com.mcatk.guildmanager;

import com.mcatk.guildmanager.command.GuildCommand;
import com.mcatk.guildmanager.command.GuildCommandS;
import com.mcatk.guildmanager.core.config.DbConfig;
import com.mcatk.guildmanager.core.repository.GuildRepository;
import com.mcatk.guildmanager.core.repository.jdbc.JdbcGuildRepository;
import com.mcatk.guildmanager.core.service.GuildService;
import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.papi.GuildPapi;
import com.mcatk.guildmanager.sql.SQLUpdater;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class GuildManager extends JavaPlugin {

    private static GuildManager plugin;
    private static Economy econ;
    private GuildService guildService;

    public static GuildManager getPlugin() {
        return plugin;
    }

    public GuildService getGuildService() {
        return guildService;
    }

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        initGuildService();
        registerDependency();
        registerCommand();
        registerListener();
        new SQLUpdater().run();
        getLogger().info("公会管理插件已启动");
    }

    @Override
    public void onDisable() {
        getLogger().info("公会管理插件已关闭");
    }

    private void registerDependency() {
        //检测前置插件
        if (!setupEconomy()) {
            getLogger().warning("未找到前置插件Vault");
        }
        getLogger().info("检测到Vault，成功启动依赖");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildPapi().register();
            getLogger().info("检测到PlaceholderAPI，已启动PAPI变量");
        }
    }

    private void registerCommand() {
        Bukkit.getPluginCommand("guildmanager").
                setExecutor(new GuildCommand(this, getGuildService()));
        Bukkit.getPluginCommand("guildmanagers").
                setExecutor(new GuildCommandS());
        getLogger().info("注册指令注册完毕");
    }

    private void registerListener() {
        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
            Bukkit.getPluginManager().registerEvents(new LoginListener(), this);
            getLogger().info("检测到AuthMe，已启动LoginEvent监听");
        }
        Bukkit.getPluginManager().registerEvents(new GuildItem(), this);
        getLogger().info("监听器注册完毕");
    }

    public void tpAll(Guild guild, Player player) {
        for (Player p :
                getServer().getOnlinePlayers()) {
            Guild targetGuild = guildService.getPlayerGuild(p.getName());
            if (targetGuild != null && guild.getId().equals(targetGuild.getId())) {
                player.chat("/tpahere " + p.getName());
            }
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().
                        getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean takePlayerMoney(Player p, double m) {
        EconomyResponse r = econ.withdrawPlayer(p, m);
        return r.transactionSuccess();
    }

    private void initGuildService() {
        String ip = getConfig().getString("mysql.ip");
        String databaseName = getConfig().getString("mysql.databasename");
        String userName = getConfig().getString("mysql.username");
        String userPassword = getConfig().getString("mysql.password");
        int port = getConfig().getInt("mysql.port");
        DbConfig dbConfig = new DbConfig(ip, port, databaseName, userName, userPassword);
        GuildRepository repository = new JdbcGuildRepository(dbConfig);
        guildService = new GuildService(repository);
    }

}
