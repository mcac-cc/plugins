package com.mcatk.gemshop.shops.vipshop;

import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.Message;
import com.mcatk.gemshop.ServerCmd;
import org.bukkit.entity.Player;

public class VipShop {
    public void buyVip(Player player, String vipType) {
        int price = getPrice(vipType);
        if (price < 0) {
            player.sendMessage(Message.ERROR + "无效的VIP类型");
            return;
        }
        GemShop.getPlugin().getPurchases().buy(player, price, () -> {
            if (player.hasPermission("cmi.command.se")) {
                player.sendMessage(Message.INFO + "已拥有VIP");
                return false;
            }
            return true;
        }, () -> ServerCmd.sendConsoleCmd(String.format(
                "lp user %s parent addtemp %s 30d", player.getName(), vipType)));
    }

    private int getPrice(String vipType) {
        switch (vipType) {
            case "vip": return 10;
            case "svip": return 20;
            case "mvp": return 30;
            default: return -1;
        }
    }
}
