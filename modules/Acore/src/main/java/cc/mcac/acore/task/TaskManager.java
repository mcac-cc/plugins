package cc.mcac.acore.task;

import cc.mcac.acore.Acore;
import cc.mcac.acore.config.PluginConfig;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public class TaskManager {
    
    private final Acore plugin;
    private final Logger logger;
    private ScheduledTask playerListTask;
    
    public TaskManager(Acore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void startAllTasks() {
        PluginConfig config = plugin.getPluginConfig();

        if (config.playerListToDbInterval <= 0) {
            logger.warn("Skipping player list task because player-list-to-db-interval is invalid: {}", config.playerListToDbInterval);
            return;
        }

        String serverId = config.serverId == null ? "" : config.serverId.trim();
        if (serverId.isEmpty()) {
            logger.warn("Skipping player list task because server-id is blank");
            return;
        }

        playerListTask = plugin.getServer().getScheduler()
                .buildTask(plugin, new PlayerListTask(plugin))
                .repeat(config.playerListToDbInterval, TimeUnit.SECONDS)
                .schedule();

        logger.info("Scheduled tasks started (player list interval: {}s)", config.playerListToDbInterval);
    }
    
    public void stopAllTasks() {
        if (playerListTask != null) {
            playerListTask.cancel();
            playerListTask = null;
            logger.info("All scheduled tasks stopped");
        }
    }

}