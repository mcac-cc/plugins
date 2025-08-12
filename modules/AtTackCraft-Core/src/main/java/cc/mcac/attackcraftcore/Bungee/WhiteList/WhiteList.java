package cc.mcac.attackcraftcore.Bungee.WhiteList;

import cc.mcac.attackcraftcore.ACBungee;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;

import static net.md_5.bungee.event.EventPriority.HIGHEST;

public class WhiteList implements Listener {

    private final ACBungee plugin;
    private final HashSet<String> whiteList;

    public WhiteList(ACBungee plugin) {
        this.plugin = plugin;
        whiteList = new HashSet<>();
        if (plugin.getConfiguration().getBoolean("whiteList")) {
            on();
        } else {
            off();
        }
    }

    @EventHandler(priority = HIGHEST)
    public void onPlayerJoin(PreLoginEvent e) {
        if (plugin.getConfiguration().getBoolean("whitelist")) {
            if (!whiteList.contains(e.getConnection().getName())) {
                e.setCancelReason(new TextComponent("§c您不在白名单中, 请在群14603699申请白名单"));
                e.setCancelled(true);
                plugin.getLogger().info("玩家" + e.getConnection().getName() + "不在白名单中, 断开连接");
            }
        }
    }

    public void on() {
        Connection connection = plugin.getSqlManager().getConnection();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM `whitelist`"
            );
            ResultSet rs = ps.executeQuery();
            whiteList.clear();
            while (rs.next()) {
                whiteList.add(rs.getString("player_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void off() {
        whiteList.clear();
    }

    public void addPlayer(String playerName) {
        whiteList.add(playerName);
        Connection connection = plugin.getSqlManager().getConnection();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `whitelist` (`player_name`) VALUES (?) ON DUPLICATE KEY UPDATE `player_name` = ?"
            );
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            ps.executeUpdate();
            whiteList.add(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return whiteList.toString();
    }
}
