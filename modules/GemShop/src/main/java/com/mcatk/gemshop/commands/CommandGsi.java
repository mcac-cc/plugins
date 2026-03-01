package com.mcatk.gemshop.commands;

import com.mcatk.gemshop.GemShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGsi implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            return false;
        }
        if (args.length == 0) {
            printHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "add":
                if (args.length != 4) {
                    printHelp(sender);
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("该命令仅玩家可执行");
                    return true;
                }
                GemShop.getPlugin().getShopFactory().getItemShop()
                        .addItem((Player) sender, args[1], args[2], args[3]);
                return true;
            case "del":
                if (args.length != 3) {
                    printHelp(sender);
                    return true;
                }
                GemShop.getPlugin().getShopFactory().getItemShop()
                        .delItem(args[1], args[2]);
                return true;
            case "list":
                if (args.length != 1) {
                    printHelp(sender);
                    return true;
                }
                sender.sendMessage(GemShop.getPlugin().getShopFactory().getItemShop().toString());
                return true;
            default:
                printHelp(sender);
                return true;
        }
    }

    void printHelp(CommandSender sender) {
        sender.sendMessage("帮助：无设置判错机制，严格按照格式执行");
        sender.sendMessage("上架物品：/gsi add <商店ID> <物品ID> <价格>");
        sender.sendMessage("删除物品：/gsi del <商店ID> <物品ID>");
    }
}
