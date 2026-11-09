package com.mcatk.guildmanager.command;

import com.mcatk.guildmanager.GuildManager;
import com.mcatk.guildmanager.Msg;
import com.mcatk.guildmanager.core.service.GuildService;
import com.mcatk.guildmanager.gui.GuildsGUI;
import com.mcatk.guildmanager.models.ApplicantsList;
import com.mcatk.guildmanager.models.Guild;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Locale;

public class GuildCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/gmg gui | apply <ID> | create <ID> | quit | offer <AC点>");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("该指令只能由玩家执行"); return true;
        }
        Player player = (Player) sender;
        GuildManager plugin = GuildManager.getPlugin();
        GuildService service = plugin.getGuildService();
        if (!service.isReady()) {
            sender.sendMessage("§e公会数据正在加载，请稍候"); return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        String name = player.getName();
        Guild guild = service.getPlayerGuild(name);
        switch (action) {
            case "gui": new GuildsGUI().openGUI(player); return true;
            case "apply":
                if (args.length != 2) break;
                if (guild != null) { player.sendMessage(Msg.ERROR + "已有公会"); return true; }
                String target = args[1];
                if (service.getGuild(target) == null) { player.sendMessage(Msg.ERROR + "不存在公会"); return true; }
                if (ApplicantsList.getApplicantsList().getList(target).contains(name)) {
                    player.sendMessage(Msg.ERROR + "今天已经申请过公会，明天再试"); return true;
                }
                ApplicantsList.getApplicantsList().getList(target).add(name);
                player.sendMessage(Msg.INFO + "申请成功，等待通过"); return true;
            case "create":
                if (args.length != 2) break;
                String id = args[1];
                if (!id.matches("[a-z]{1,32}")) { player.sendMessage("§c公会ID须为1至32位小写英文字母"); return true; }
                if (guild != null) { player.sendMessage("§c你已有公会"); return true; }
                if (service.getGuild(id) != null) { player.sendMessage("§c公会ID已存在，未扣款"); return true; }
                plugin.getPayments().charge(player, "create:" + id, 1000000,
                        () -> service.canCreateGuild(id, name), () -> service.createGuild(id, name), "§a公会创建成功");
                return true;
            case "offer":
                if (args.length != 2) break;
                if (guild == null) { player.sendMessage("§c你没有公会"); return true; }
                final int amount;
                try { amount = Integer.parseInt(args[1]); }
                catch (NumberFormatException e) { player.sendMessage("§c金额必须为整数"); return true; }
                if (amount <= 0 || amount % 10000 != 0) { player.sendMessage("§c金额必须为10000的正整数倍"); return true; }
                String guildId = guild.getId();
                plugin.getPayments().charge(player, "donate:" + name, amount,
                        () -> service.canDonate(name, guildId), () -> service.donate(name, guildId, amount),
                        "§a已捐赠 " + amount + " AC点，贡献值上限为100");
                return true;
            case "quit":
                if (guild == null) { player.sendMessage("§c你没有公会"); return true; }
                if (guild.isManager(name)) { player.sendMessage("请先撤销你的公会职务"); return true; }
                String oldGuild = guild.getId();
                plugin.queryAsync(() -> {
                    service.removeMember(name, oldGuild); service.refresh(); return true;
                }, ignored -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "res pset main.gh " + name + " move remove");
                    if (player.isOnline()) player.sendMessage("§a已退出公会");
                }, error -> { if (player.isOnline()) player.sendMessage("§c操作失败，请稍后核对公会状态"); });
                return true;
            default:
        }
        player.sendMessage("§c参数错误，请输入 /gmg 查看帮助");
        return true;
    }
}
