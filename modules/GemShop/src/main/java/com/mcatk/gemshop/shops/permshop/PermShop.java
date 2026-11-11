package com.mcatk.gemshop.shops.permshop;

import com.mcatk.gemshop.GemShop;
import com.mcatk.gemshop.Product;
import com.mcatk.gemshop.Message;
import com.mcatk.gemshop.ServerCmd;
import org.bukkit.entity.Player;
import java.util.HashMap;

public class PermShop {
    private final HashMap<String, Integer> permPrice = new HashMap<>();
    public PermShop() {
        permPrice.put("dragon.use.10", 10);
        permPrice.put("dragon.use.11", 20);
        permPrice.put("dragon.use.12", 30);
        permPrice.put("z.island.biomes", 10);
    }

    public void buyPerm(Player player, String permission) {
        Integer price = permPrice.get(permission);
        if (price == null) {
            player.sendMessage(Message.ERROR + "无该权限");
            return;
        }
        GemShop.getPlugin().getPurchases().buy(player, price, Product.permission(permission), () -> {
            if (player.hasPermission(permission)) {
                player.sendMessage(Message.INFO + "已拥有该权限");
                return false;
            }
            return true;
        }, () -> ServerCmd.addPermission(player.getName(), permission));
    }
}
