package com.mcatk.medalcabinet.command;

import com.mcatk.medalcabinet.MedalCabinet;
import com.mcatk.medalcabinet.medal.Medal;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Locale;

public class MedalAdminCmd implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (!sender.isOp()) return false;
        String[] args = arguments.clone();
        if (args.length == 0) {
            sender.sendMessage("/medaladmin create <ID> <名称> <材质> <说明> | add <玩家> <ID> | check <玩家> | list");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        int length = "create".equals(action) ? 5 : "add".equals(action) ? 3
                : "check".equals(action) ? 2 : "list".equals(action) ? 1 : -1;
        if (args.length != length) {
            sender.sendMessage("§c参数错误，请输入 /medaladmin 查看帮助");
            return true;
        }
        MedalCabinet.getPlugin().queryAsync(() -> {
            SQLManager sql = SQLManager.getInstance();
            switch (action) {
                case "create": sql.createMedal(args[1], args[2], args[3], args[4]); return "Ok";
                case "add": sql.addPlayerMedal(args[1], args[2]); return "Ok";
                case "check":
                    StringBuilder playerMedals = new StringBuilder(args[1]).append("的勋章：\n");
                    for (Medal medal : sql.getPlayerMedals(args[1])) playerMedals.append(medal).append(' ');
                    return playerMedals.toString();
                default:
                    StringBuilder medals = new StringBuilder();
                    for (Medal medal : sql.getAllMedals()) medals.append(medal).append('\n');
                    return medals.toString();
            }
        }, sender::sendMessage, error -> sender.sendMessage("§c数据库操作失败，请稍后重试"));
        return true;
    }
}
