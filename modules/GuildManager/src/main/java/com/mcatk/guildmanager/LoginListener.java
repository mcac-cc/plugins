package com.mcatk.guildmanager;

import com.mcatk.guildmanager.models.Guild;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LoginListener implements Listener {

    @EventHandler
    public void onAuth(LoginEvent e) {
        Player player = e.getPlayer();
        Guild guild = GuildManager.getPlugin().getGuildService().getPlayerGuild(player.getName());
        if (guild != null) {
            // 玩家有公会
            if (guild.getChairman().equals(player.getName())) {
                // 会长 全部公告
                GuildManager.getPlugin().getServer().broadcastMessage(
                        Msg.INFO + "§6" + guild.getGuildName() + " §7会长 §e" + player.getName() + " §7已上线"
                );
            } else {
                // 非会长 会内公告
                // Optimize player notifications in LoginListener
                // Iterates over specific member list and uses Bukkit.getPlayerExact to check online status (O(M))
                // rather than iterating over all online players and searching the member list each time (O(N))
                for (String memberName : GuildManager.getPlugin().getGuildService().getGuildMembers(guild.getId())) {
                    Player p = Bukkit.getPlayerExact(memberName);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(
                                Msg.INFO + "§6" + guild.getGuildName() + " §7成员 §e" + player.getName() + " §7已上线"
                        );
                    }
                }
            }
        }
    }
}
