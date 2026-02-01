package cc.mcac.acore.bot;


import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.velocity.event.message.passive.MiraiGroupMessageEvent;

import java.util.Collection;

public class MessageListener {

    private final ProxyServer server;

    public MessageListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onGroupMessageReceive(MiraiGroupMessageEvent e) {
        if (e.getMessage().equals("#ls")) {
            MiraiBot.getBot(e.getBotID()).getGroup(e.getGroupID()).sendMessage(getOnlinePlayerListMsg());
        }
    }


    private String getOnlinePlayerListMsg() {
        Collection<Player> players = server.getAllPlayers();
        if (players.isEmpty()) {
            return "当前没有在线玩家";
        }
        StringBuilder onlinePlayerListMsg = new StringBuilder();
        onlinePlayerListMsg.append("当前在线人数: ");
        onlinePlayerListMsg.append(server.getAllPlayers().size()).append("\n");
        onlinePlayerListMsg.append("在线玩家: ");
        server.getAllPlayers().forEach(player -> onlinePlayerListMsg.append(player.getUsername()).append(", "));
        onlinePlayerListMsg.setLength(onlinePlayerListMsg.length() - 2); // Remove the last comma and space
        return onlinePlayerListMsg.toString();
    }
}
