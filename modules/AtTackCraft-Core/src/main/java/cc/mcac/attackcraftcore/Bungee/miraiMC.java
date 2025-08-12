package cc.mcac.attackcraftcore.Bungee;

import cc.mcac.attackcraftcore.ACBungee;
import cc.mcac.attackcraftcore.Bungee.WhiteList.WhiteList;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.bungee.event.message.passive.MiraiGroupMessageEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;


public class miraiMC implements Listener {
    private final ACBungee plugin;

    private final WhiteList whiteList;

    public miraiMC(ACBungee plugin, WhiteList whiteList) {
        this.plugin = plugin;
        this.whiteList = whiteList;
    }

    @EventHandler
    public void onGroupMessageReceive(MiraiGroupMessageEvent e) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            if (e.getMessage().startsWith("##")) {
                plugin.getProxy().broadcast(new TextComponent(
                        "§7[§6§l群消息§7]" + "§7[§e" + e.getSenderName() + "§7]: " + e.getMessage().substring(2)));
            } else if (e.getMessage().equals("#ls")) {
                StringBuilder playerListMsg = new StringBuilder(String.format("当前在线人数: %s", plugin.getProxy().getOnlineCount()));
                plugin.getProxy().getServers().forEach((serverName, serverInfo) -> {
                    if (!serverInfo.getPlayers().isEmpty()) {
                        playerListMsg.append(String.format("\n%s[%s]: [", getServerNickname(serverName), serverInfo.getPlayers().size()));
                        serverInfo.getPlayers().forEach(player -> playerListMsg.append(player.getName()).append(", "));
                        playerListMsg.delete(playerListMsg.length() - 2, playerListMsg.length());
                        playerListMsg.append("]");
                    }
                });
                MiraiBot.getBot(plugin.getConfiguration().getLong("mirai.qq")).getGroup(plugin.getConfiguration().getLong("mirai.group"))
                        .sendMessage(playerListMsg.toString());
            } else if (e.getMessage().startsWith("#申请白名单 ")) {
                String playerName = e.getMessage().split(" ")[1];
                whiteList.addPlayer(playerName);
                MiraiBot.getBot(plugin.getConfiguration().getLong("mirai.qq")).getGroup(plugin.getConfiguration().getLong("mirai.group"))
                        .sendMessage(String.format("已将%s加入白名单", playerName));
            }
        });
    }

    @EventHandler
    public void onBungeeCordMessageReceive(ChatEvent e) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            if (e.getMessage().startsWith("##")) {
                ProxiedPlayer player = (ProxiedPlayer) e.getSender();
                MiraiBot.getBot(plugin.getConfiguration().getLong("mirai.qq")).getGroup(plugin.getConfiguration().getLong("mirai.group"))
                        .sendMessage("[" + getServerNickname(player.getServer().getInfo().getName()) + "]" + "[" + e.getSender() + "]: " + e.getMessage().substring(2));
            }
        });
    }

    private String getServerNickname(String serverName) {
        switch (serverName) {
            case "lobby":
                return "大厅";
            case "mcac":
                return "主服";
            case "survival":
                return "生存服";
            case "island":
                return "空岛服";
            case "game":
                return "小游戏";
        }
        return serverName;
    }
}
