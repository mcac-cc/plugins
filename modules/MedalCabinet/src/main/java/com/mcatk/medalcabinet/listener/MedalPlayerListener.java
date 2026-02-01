package com.mcatk.medalcabinet.listener;

import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class MedalPlayerListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        SQLManager.getInstance().clearCache(e.getPlayer().getName());
    }
}
