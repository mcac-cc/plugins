package com.mcatk.itemmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemManagerCommand implements CommandExecutor {
    private CommandSender sender;
    private String[] args;

    void printHelp() {
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
        this.sender = sender;
        this.args = args;
        if (args.length == 0) {
            printHelp();
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create":
                return create();
            case "add":
                return add();
            case "get":
            case "give":
                return give();
            case "list":
                list();
                return true;
            default:
                printHelp();
                return true;
        }
    }

    private boolean create() {
        if (args.length < 2) {
            sender.sendMessage("用法: /im create <类型ID>");
            return true;
        }

        String sortId = args[1];
        ItemManager.getItemSort().createSort(sortId);
        sender.sendMessage("Ok");
        return true;
    }

    private boolean add() {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("用法: /im add <类型ID> <商品ID>");
            return true;
        }

        String sortId = args[1];
        String itemId = args[2];
        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType().isAir()) {
            sender.sendMessage("请先手持要保存的物品。");
            return true;
        }

        ItemManager.getItemSort().addItem(sortId, itemId, itemInHand);
        sender.sendMessage("Ok");
        return true;
    }

    private boolean give() {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("用法: /im get <类型ID> <商品ID>");
            return true;
        }

        String sortId = args[1];
        String itemId = args[2];
        ItemStack item = ItemManager.getItemSort().getItem(sortId, itemId);
        if (item == null) {
            sender.sendMessage("未找到对应物品: " + sortId + "/" + itemId);
            return true;
        }

        ((Player) sender).getInventory().addItem(item.clone());
        sender.sendMessage("Ok");
        return true;
    }

    private void list() {
        sender.sendMessage(ItemManager.getItemSort().listAll());
    }

}
