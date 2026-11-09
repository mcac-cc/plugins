package com.mcatk.medalcabinet.listener;

import com.mcatk.medalcabinet.MedalCabinet;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class MedalPlayerListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        MedalCabinet.getPlugin().clearTitle(e.getPlayer().getName());
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        MedalCabinet.getPlugin().requestTitle(e.getPlayer().getName());
    }
}
