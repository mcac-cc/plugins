package com.mcatk.gem.command;

import com.mcatk.gem.Gem;
import com.mcatk.gem.GemExecutor;
import com.mcatk.gem.Message;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Locale;

public class CommandGem implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        String[] args = arguments.clone();
        String name = sender.getName();
        if (!sender.isOp()) {
            Gem.getPlugin().queryAsync(() -> Gem.getPlugin().getGemExecutor().getGems(name),
                    value -> sender.sendMessage(Message.INFO + "你的宝石：" + value),
                    error -> sender.sendMessage(Message.ERROR + "数据库暂不可用，请稍后重试"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/gem set|add|take <玩家> <正整数> | check|total|delete <玩家>");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        boolean amountRequired = "set".equals(action) || "add".equals(action) || "take".equals(action);
        boolean playerOnly = "check".equals(action) || "total".equals(action) || "delete".equals(action);
        if ((!amountRequired && !playerOnly) || args.length != (amountRequired ? 3 : 2)) {
            sender.sendMessage(Message.ERROR + "参数长度有误");
            return true;
        }
        final int amount;
        try {
            amount = amountRequired ? Integer.parseInt(args[2]) : 0;
            if (amountRequired && amount <= 0) throw new IllegalArgumentException();
        } catch (NumberFormatException e) {
            sender.sendMessage(Message.ERROR + "宝石必须是整数");
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Message.ERROR + "宝石数量必须大于 0");
            return true;
        }
        String target = args[1];
        Gem.getPlugin().queryAsync(() -> {
            GemExecutor api = Gem.getPlugin().getGemExecutor();
            switch (action) {
                case "set": api.setGems(target, amount); return target + " 的宝石设置为： " + amount;
                case "add": api.addGems(target, amount); return target + " 增加了 " + amount + " 宝石";
                case "take": return api.takeGems(target, amount) ? target + " 减少 " + amount + " 宝石" : target + " 宝石不足";
                case "delete": return api.deleteData(target) ? target + " 宝石数据已删除" : target + " 没有可删除的宝石数据";
                case "total": return target + " 累计 " + api.getTotalGems(target) + " 宝石";
                default: return target + " 现在有 " + api.getGems(target) + " 宝石";
            }
        }, result -> {
            sender.sendMessage(Message.INFO + result);
            if ("add".equals(action)) {
                Player player = Bukkit.getPlayerExact(target);
                if (player != null) player.sendMessage("收到 " + amount + " 宝石");
            }
        },
                error -> sender.sendMessage(Message.ERROR + "数据库操作结果异常，请核对后再操作，勿重复提交"));
        return true;
    }
}
