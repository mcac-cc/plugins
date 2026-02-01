package cc.mcac.acore;

import cc.mcac.acore.bot.MessageListener;
import cc.mcac.acore.config.ConfigManager;
import cc.mcac.acore.config.PluginConfig;
import cc.mcac.acore.database.DatabaseManager;
import cc.mcac.acore.task.TaskManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "acore",
        name = "Acore",
        version = BuildConstants.VERSION
        , authors = {"acsoto"}
)
public class Acore {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private final Metrics.Factory metricsFactory;
    private TaskManager taskManager;


    @Inject
    public Acore(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        logger.info("Acore plugin initialized with version: {}", BuildConstants.VERSION);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadConfig();
        loadDatabase();
        setUpBStats();
        server.getEventManager().register(this, new MessageListener(server));
        startTasks();
    }

    private void startTasks() {
        this.taskManager = new TaskManager(this);
        taskManager.startAllTasks();
    }

    private void loadConfig() {
        this.configManager = new ConfigManager(logger, dataDirectory);
        logger.info("Acore config loaded on server: {}", configManager.getPluginConfig().serverId);
    }

    public PluginConfig getPluginConfig() {
        return this.configManager.getPluginConfig();
    }

    public Logger getLogger() {
        return logger;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ProxyServer getServer() {
        return server;
    }

    private void loadDatabase() {
        this.databaseManager = new DatabaseManager(this);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (taskManager != null) {
            taskManager.stopAllTasks();
        }
        databaseManager.shutdown();
        logger.info("Database closed.");
    }

    private void setUpBStats() {
        int pluginId = 27159;
        Metrics metrics = metricsFactory.make(this, pluginId);
    }
}

