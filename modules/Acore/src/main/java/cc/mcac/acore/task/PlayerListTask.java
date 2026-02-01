package cc.mcac.acore.task;

import cc.mcac.acore.Acore;
import com.velocitypowered.api.proxy.Player;

import java.util.stream.Collectors;

public class PlayerListTask implements Runnable {

    private final Acore acore;

    public PlayerListTask(Acore acore) {
        this.acore = acore;
    }

    @Override
    public void run() {
        String serverId = acore.getPluginConfig().serverId;
        int playerCount = acore.getServer().getPlayerCount();
        String playerList = acore.getServer().getAllPlayers().stream()
                .map(Player::getUsername)
                .collect(Collectors.joining(","));
        acore.getDatabaseManager().updatePlayerList(serverId, playerCount, playerList);
    }

}
