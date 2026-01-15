package com.mcatk.itemmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemManagerCommand implements CommandExecutor {
    
    void printHelp(CommandSender sender) {
        sender.sendMessage("帮助：严格按照格式执行");
        sender.sendMessage("创建类型: /im create <类型ID>");
        sender.sendMessage("加入物品: /im add <类型ID> <商品ID>");
        sender.sendMessage("获取物品: /im get <类型ID> <商品ID>");
        sender.sendMessage("列出物品: /im list");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            return false;
        }
        if (args.length == 0) {
            printHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create":
                create(sender, args);
                break;
            case "add":
                add(sender, args);
                break;
            case "get":
                get(sender, args);
                break;
            case "list":
                list(sender);
                break;
            default:
        }
        return true;
    }
    
    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("参数不足: /im create <类型ID>");
            return;
        }
        String sortId = args[1];
        ItemManager.getItemSort().createSort(sortId);
        sender.sendMessage("Ok");
    }
    
    private void add(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("参数不足: /im add <类型ID> <商品ID>");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以执行此命令");
            return;
        }
        String sortId = args[1];
        String itemId = args[2];
        ItemManager.getItemSort().addItem(sortId, itemId,
                ((Player) sender).getInventory().getItemInMainHand());
        sender.sendMessage("Ok");
    }
    
    private void get(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("参数不足: /im get <类型ID> <商品ID>");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以执行此命令");
            return;
        }
        String sortId = args[1];
        String itemId = args[2];
        ItemStack item = ItemManager.getItemSort().getItem(sortId, itemId);
        if (item != null) {
            ((Player) sender).getInventory().addItem(item);
            sender.sendMessage("Ok");
        } else {
            sender.sendMessage("物品不存在");
        }
    }
    
    private void list(CommandSender sender) {
        sender.sendMessage(ItemManager.getItemSort().listAll());
    }
    
}
