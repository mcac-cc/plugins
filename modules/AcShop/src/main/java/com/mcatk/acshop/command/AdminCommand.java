package com.mcatk.acshop.command;

import com.mcatk.acshop.AcShop;
import com.mcatk.acshop.FileOperation;
import com.mcatk.acshop.commodity.Item;
import com.mcatk.acshop.commodity.ItemType;
import com.mcatk.acshop.shop.Shop;
import com.mcatk.itemmanager.ItemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class AdminCommand implements CommandExecutor {

    private static final String HELP_INVALID_USAGE = "参数错误，请按帮助格式执行";
    private static final String HELP_INVALID_PRICE = "价格必须是大于 0 的整数";
    private static final String HELP_PLAYER_ONLY = "只有玩家可以使用 /asadmin add item";
    private static final String HELP_SHOP_NOT_FOUND = "商店不存在：";
    private static final String HELP_ITEM_NOT_FOUND = "商品不存在：";

    private CommandSender sender;
    private String[] args;

    void printHelp() {
        sender.sendMessage("帮助：无设置判错机制，严格按照格式执行");
        sender.sendMessage("上架物品：/asadmin add <item/cmd> <商店ID> <商品ID> <价格> ");
        sender.sendMessage("设置：/asadmin setcmd <商店ID> <商品ID> <cmd>");
        sender.sendMessage("重载：/asadmin reload ");
        //sender.sendMessage("删除物品：/acshop del <商店ID> <物品ID>");
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
        boolean changed = false;
        switch (args[0].toLowerCase()) {
            case "add":
                changed = add();
                break;
            case "reload":
                AcShop.getPlugin().loadShops();
                break;
            case "setcmd":
                changed = setCmd();
                break;
            default:
                printHelp();
                break;
        }
        if (changed) {
            new FileOperation().saveShops(AcShop.getShops());
        }
        return true;
    }
    
    private boolean add() {
        if (args.length < 5) {
            sender.sendMessage(HELP_INVALID_USAGE);
            printHelp();
            return false;
        }

        String shopType = args[1].toLowerCase();
        if (!"item".equals(shopType) && !"cmd".equals(shopType)) {
            sender.sendMessage(HELP_INVALID_USAGE);
            printHelp();
            return false;
        }

        String shopId = args[2];
        if (!AcShop.getShops().getShopsHashMap().containsKey(shopId)) {
            AcShop.getShops().getShopsHashMap().put(shopId, new Shop(shopId));
        }
        String itemId = args[3];
        int price = parsePositivePrice(args[4]);
        if (price <= 0) {
            sender.sendMessage(HELP_INVALID_PRICE);
            return false;
        }
        switch (shopType) {
            case "item":
                return addItemStackItem(shopId, itemId, price);
            case "cmd":
                return addCmdItem(shopId, itemId, price);
            default:
                return false;
        }
    }
    
    private boolean addItemStackItem(String shopId, String itemId, int price) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(HELP_PLAYER_ONLY);
            return false;
        }

        // 商品类型固定为保留字 Shop_<shopId>
        String sortId = "Shop_" + shopId;
        ItemManager.addItem(sortId, itemId,
                ((Player) sender).getInventory().getItemInMainHand());
        AcShop.getShops().getShopsHashMap().get(shopId).getItemHashMap().put(
                itemId, new Item(ItemType.ITEM_STACK, itemId, price, sortId, itemId)
        );
        sender.sendMessage("Ok");
        return true;
    }

    private boolean addCmdItem(String shopId, String itemId, int price) {
        AcShop.getShops().getShopsHashMap().get(shopId).getItemHashMap().put(
                itemId, new Item(ItemType.ITEM_CMD, itemId, price, "")
        );
        sender.sendMessage("Ok");
        return true;
    }

    private boolean setCmd() {
        if (args.length < 4) {
            sender.sendMessage(HELP_INVALID_USAGE);
            printHelp();
            return false;
        }

        String shopId = args[1];
        Map<String, Item> items = getItems(shopId);
        if (items == null) {
            sender.sendMessage(HELP_SHOP_NOT_FOUND + shopId);
            return false;
        }

        String itemId = args[2];
        Item item = items.get(itemId);
        if (item == null) {
            sender.sendMessage(HELP_ITEM_NOT_FOUND + itemId);
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            stringBuilder.append(args[i]).append(" ");
        }
        item.setCmd(stringBuilder.toString().trim());
        sender.sendMessage("Ok");
        return true;
    }

    private int parsePositivePrice(String rawPrice) {
        try {
            return Integer.parseInt(rawPrice);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private Map<String, Item> getItems(String shopId) {
        Shop shop = AcShop.getShops().getShopsHashMap().get(shopId);
        if (shop == null) {
            return null;
        }
        return shop.getItemHashMap();
    }
    
}
