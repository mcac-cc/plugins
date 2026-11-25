package com.mcatk.gemshop.commands;

import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.Message;
import com.mcatk.gemshop.shops.itemshop.ItemShopGui;
import com.mcatk.gemshop.shops.itemshop.Items;
import com.mcatk.gemshop.shops.itemshop.ItemShop;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGemShop implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (args.length == 0) {
                return true;
            } else if (args.length != 2) {
                sender.sendMessage("参数错误");
                return true;
            }
            Player player = (Player) sender;
            String type = args[0].toLowerCase(Locale.ROOT);
            String id = args[1].toLowerCase(Locale.ROOT);
            switch (type) {
                case "vip":
                    GemShop.getPlugin().getShopFactory().getVipShop().buyVip(player, id);
                    break;
                case "perm":
                    GemShop.getPlugin().getShopFactory().getPermShop().buyPerm(player, id);
                    break;
                case "item":
                    ItemShop shop = GemShop.getPlugin().getShopFactory().getItemShop();
                    Items items = shop.getItems(args[1]);
                    if (items == null && !args[1].equals(id)) {
                        items = shop.getItems(id);
                    }
                    if (items == null) {
                        sender.sendMessage(Message.ERROR + "无该分类");
                        break;
                    }
                    player.openInventory(new ItemShopGui(items).getGui());
                    break;
                default:
                    sender.sendMessage("gemshop vip/perm/item");
                    break;
            }
        } else {
            sender.sendMessage("非玩家不可执行");
        }
        return true;
    }
}
