package com.mcatk.guildmanager.sql;

import com.mcatk.guildmanager.GuildManager;
import org.bukkit.Bukkit;

public class SQLUpdater {
    public void run() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                GuildManager.getPlugin(),
                () -> {
                    try { GuildManager.getPlugin().getGuildService().refresh(); }
                    catch (RuntimeException e) {
                        GuildManager.getPlugin().getLogger().log(java.util.logging.Level.WARNING,
                                "Guild cache refresh failed; retaining previous snapshot", e);
                    }
                },
                0L,
                20L * 60L
        );
    }
}
