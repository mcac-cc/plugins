package com.mcatk.medalcabinet.command;

import com.mcatk.medalcabinet.MedalCabinet;
import com.mcatk.medalcabinet.medal.Medal;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MedalShowCmd implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        if (args.length != 1 || !("all".equals(args[0]) || "me".equals(args[0]))) {
            sender.sendMessage("/medalshow all 全服展示勋章 | /medalshow me 查看自己的勋章");
            return true;
        }
        Player player = (Player) sender;
        String name = player.getName();
        boolean broadcast = "all".equals(args[0]);
        MedalCabinet.getPlugin().queryAsync(() -> SQLManager.getInstance().getPlayerMedals(name), medals -> {
            StringBuilder text = new StringBuilder("§7[§6勋章墙§7] §e").append(name).append(" 的勋章：\n");
            for (Medal medal : medals) text.append(medal).append(' ');
            if (broadcast) Bukkit.broadcastMessage(text.toString());
            else if (player.isOnline()) player.sendMessage(text.toString());
        }, error -> {
            if (player.isOnline()) player.sendMessage("§c勋章数据读取失败，请稍后重试");
        });
        return true;
    }
}
