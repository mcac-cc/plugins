package com.mcatk.acshop;

import com.mcatk.acshop.commodity.Item;
import com.mcatk.acshop.commodity.ItemType;
import com.mcatk.itemmanager.ItemManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Operation {
    
    public void buy(Player player, Item item) {
        if (item == null || item.getPrice() < 0) {
            player.sendMessage("§c商品配置无效，未扣款");
            return;
        }
        if (item.getType() != ItemType.ITEM_STACK) {
            player.sendMessage("§c命令商品已停用，未扣款");
            return;
        }
        ItemStack template = ItemManager.getItem(item.getSortId(), item.getItemId());
        ItemStack delivery = template == null ? null : template.clone();
        if (!InventoryDelivery.canFit(player.getInventory(), delivery)) {
            player.sendMessage("§c商品不可交付或背包空间不足，未扣款");
            return;
        }
        if (!VaultApi.takePlayerMoney(player, item.getPrice())) {
            player.sendMessage("AC点不足");
            return;
        }
        try {
            if (!InventoryDelivery.deliver(player.getInventory(), delivery)) {
                if (!VaultApi.refundPlayerMoney(player, item.getPrice())) {
                    AcShop.getPlugin().getLogger().severe("REFUND REQUIRED: " + player.getName() + " AC=" + item.getPrice() + " item=" + item.getId());
                    player.sendMessage("§c交付失败，退款未完成，请联系管理员核对");
                } else player.sendMessage("§c交付失败，已退回 AC 点");
                return;
            }
            AcShop.getPlugin().getLogger().info(player.getName() + "购买了 " + item);
            player.sendMessage("购买成功");
        } catch (RuntimeException failure) {
            // The delivery outcome is uncertain. Do not blindly refund and duplicate value.
            AcShop.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "DELIVERY REVIEW REQUIRED: " + player.getName() + " AC=" + item.getPrice() + " item=" + item.getId(), failure);
            player.sendMessage("§c交付结果异常，请联系管理员核对本次交易");
        }
    }
}
