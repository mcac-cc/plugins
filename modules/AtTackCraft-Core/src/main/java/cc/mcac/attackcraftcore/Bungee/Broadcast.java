package cc.mcac.attackcraftcore.Bungee;

import cc.mcac.attackcraftcore.ACBungee;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Broadcast {

    private ACBungee plugin;

    private ArrayList<String> broadcastList;

    public Broadcast(ACBungee plugin) {
        this.plugin = plugin;
        broadcastList = new ArrayList<>(plugin.getConfiguration().getStringList("broadcast"));
    }

    public void run() {
        if (broadcastList.isEmpty()){
            plugin.getLogger().info("Broadcast list is emplty.");
            return;
        }
        plugin.getLogger().info("Broadcast starts.");
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            int index = (int) (Math.random() * broadcastList.size());
            ProxyServer.getInstance().broadcast(new TextComponent("§7[§6§l公告§7] §e" + broadcastList.get(index)));
        }, 1, 5, TimeUnit.MINUTES);
    }
}
