package com.mcatk.medalcabinet.command;

import com.mcatk.medalcabinet.MedalCabinet;
import com.mcatk.medalcabinet.MedalsGui;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MedalUsualCmd implements CommandExecutor {
    private final Set<UUID> loading = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        if (!loading.add(uuid)) {
            player.sendMessage("§e勋章数据正在加载，请稍候");
            return true;
        }
        String id = args.length == 0 ? player.getName() : args[0];
        Inventory previous = player.getOpenInventory().getTopInventory();
        MedalCabinet.getPlugin().queryAsync(() -> SQLManager.getInstance().getPlayerMedals(id), medals -> {
            loading.remove(uuid);
            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(previous)) {
                new MedalsGui(id, medals).openGUI(player);
            }
        }, error -> {
            loading.remove(uuid);
            if (player.isOnline()) player.sendMessage("§c勋章数据加载失败，请稍后重试");
        });
        return true;
    }
}
