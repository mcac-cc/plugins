package com.mcatk.guildmanager;

import com.mcatk.guildmanager.models.Guild;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

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
                // ⚡ Bolt: Iterate over guild members directly instead of all online players to reduce complexity from O(N*M) to O(M)
                List<String> members = GuildManager.getPlugin().getGuildService().getGuildMembers(guild.getId());
                String message = Msg.INFO + "§6" + guild.getGuildName() + " §7成员 §e" + player.getName() + " §7已上线";
                for (String memberName : members) {
                    Player p = Bukkit.getPlayerExact(memberName);
                    if (p != null) {
                        p.sendMessage(message);
                    }
                }
                    }
                }
            }
        }
    }
}
