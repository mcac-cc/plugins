package com.mcatk.guildmanager.sql;

import com.mcatk.guildmanager.GuildManager;
import org.bukkit.Bukkit;

public class SQLUpdater {
    public void run() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(GuildManager.getPlugin(), () -> SQLManager.getInstance().updateAsync(), 20L * 60L, 20L * 60L);
    }
}
