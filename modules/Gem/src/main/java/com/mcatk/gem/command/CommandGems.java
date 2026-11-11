package com.mcatk.gem.command;

import com.mcatk.gem.Gem;
import com.mcatk.gem.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGems implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("指令只能由玩家发出");
            return true;
        }
        String name = sender.getName();
        Gem.getPlugin().queryAsync(() -> Gem.getPlugin().getGemExecutor().getGems(name),
                value -> sender.sendMessage(Message.INFO + "你的宝石：" + value),
                error -> sender.sendMessage(Message.ERROR + "数据库暂不可用，请稍后重试"));
        return true;
    }
}
