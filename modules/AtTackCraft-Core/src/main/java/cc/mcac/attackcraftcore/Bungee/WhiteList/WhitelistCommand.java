package cc.mcac.attackcraftcore.Bungee.WhiteList;

import cc.mcac.attackcraftcore.ACBungee;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class WhitelistCommand extends Command {

    private final ACBungee plugin;
    private final WhiteList whiteList;

    public WhitelistCommand(ACBungee plugin, WhiteList whiteList) {
        super("whitelist");
        this.plugin = plugin;
        this.whiteList = whiteList;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer) {
            return;
        }
        if (args.length == 0) {
            sender.sendMessage(new TextComponent("§a/whitelist add <player>"));
            sender.sendMessage(new TextComponent("§a/whitelist <on/off>"));
        } else if (args[0].equalsIgnoreCase("add")) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponent("§c/whitelist add <player>"));
                return;
            }
            String playerName = args[1];
            whiteList.addPlayer(playerName);
            sender.sendMessage(new TextComponent("§a" + playerName + "已加入白名单"));
        } else if (args[0].equalsIgnoreCase("on")) {
            whiteList.on();
            plugin.getConfiguration().set("whitelist", true);
            plugin.saveConfig();
            sender.sendMessage(new TextComponent("§a白名单已开启"));
        } else if (args[0].equalsIgnoreCase("off")) {
            whiteList.off();
            plugin.getConfiguration().set("whitelist", false);
            plugin.saveConfig();
            sender.sendMessage(new TextComponent("§a白名单已关闭"));
        }
    }
}