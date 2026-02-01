package com.mcatk.guildmanager.sql;

import com.mcatk.guildmanager.GuildManager;
import org.bukkit.Bukkit;

public class SQLUpdater {
    public void run() {
        Bukkit.getScheduler().runTaskTimer(
                GuildManager.getPlugin(),
                () -> GuildManager.getPlugin().getGuildService().refresh(),
                20L * 60L,
                20L * 60L
        );
    }
}
