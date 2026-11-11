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
        ItemStack delivery = null;
        if (item.getType() == ItemType.ITEM_STACK) {
            ItemStack template = ItemManager.getItem(item.getSortId(), item.getItemId());
            delivery = template == null ? null : template.clone();
            if (!InventoryDelivery.canFit(player.getInventory(), delivery)) {
                player.sendMessage("§c商品不可交付或背包空间不足，未扣款");
                return;
            }
        } else if (item.getType() != ItemType.ITEM_CMD || item.getCmd() == null || item.getCmd().trim().isEmpty()) {
            player.sendMessage("§c商品配置无效，未扣款");
            return;
        }
        if (!VaultApi.takePlayerMoney(player, item.getPrice())) {
            player.sendMessage("AC点不足");
            return;
        }
        try {
            if (delivery != null && !InventoryDelivery.deliver(player.getInventory(), delivery)) {
                if (!VaultApi.refundPlayerMoney(player, item.getPrice())) {
                    AcShop.getPlugin().getLogger().severe("REFUND REQUIRED: " + player.getName() + " AC=" + item.getPrice() + " item=" + item.getId());
                    player.sendMessage("§c交付失败，退款未完成，请联系管理员核对");
                } else player.sendMessage("§c交付失败，已退回 AC 点");
                return;
            }
            if (delivery == null) executeCommand(player, item);
            AcShop.getPlugin().getLogger().info(player.getName() + "购买了 " + item);
            player.sendMessage("购买成功");
        } catch (RuntimeException failure) {
            // The delivery/command outcome is uncertain. Do not blindly refund and duplicate value.
            AcShop.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "DELIVERY REVIEW REQUIRED: " + player.getName() + " AC=" + item.getPrice() + " item=" + item.getId(), failure);
            player.sendMessage("§c交付结果异常，请联系管理员核对本次交易");
        }
    }

    private void executeCommand(Player player, Item item) {
        String cmd = item.getCmd();
        if (cmd.contains("%p")) {
            cmd = cmd.replace("%p", player.getName());
        }
        if (!player.isOp()) {
            try {
                player.setOp(true);
                player.chat("/" + cmd);
            } finally {
                player.setOp(false);
            }
        } else {
            player.chat("/" + cmd);
        }
    }
}
