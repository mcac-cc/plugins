package com.mcatk.gem.command;

import com.mcatk.gem.Gem;
import com.mcatk.gem.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandGem implements CommandExecutor {
    private static final String POSITIVE_AMOUNT_ERROR = "宝石数量必须大于 0";
    private CommandSender sender;
    private String[] args;

    private void printHelp() {
        sender.sendMessage("/gem set <player> <gems> 设置宝石");
        sender.sendMessage("/gem delete <player> 删除全部数据");
        sender.sendMessage("/gem check <player>  查看宝石");
        sender.sendMessage("/gem add <player> <gems> 增加宝石");
        sender.sendMessage("/gem take <player> <gems> 减少宝石");
        sender.sendMessage("/gem total <player> 累计宝石");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.sender = sender;
        this.args = args;
        if (!sender.isOp()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    int gems = Gem.getPlugin().getGemExecutor().getGems(sender.getName());
                    sender.sendMessage(Message.INFO + "你的宝石：" + gems);
                }
            }.runTaskAsynchronously(Gem.getPlugin());
            return true;
        }
        if (args.length == 0) {
            printHelp();
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "set":
                set();
                break;
            case "delete":
                delete();
                break;
            case "check":
                check();
                break;
            case "add":
                add();
                break;
            case "take":
                take();
                break;
            case "total":
                total();
                break;
            default:
                printHelp();
        }
        return true;
    }

    private void set() {
        if (args.length != 3) {
            sendParameterError();
        } else {
            try {
                int amount = parsePositiveAmount(args[2]);
                Gem.getPlugin().getGemExecutor().setGems(args[1], amount);
                sender.sendMessage(Message.INFO + args[1] + " 的宝石设置为： " + amount);
            } catch (NumberFormatException e) {
                sender.sendMessage(Message.ERROR + "宝石必须是整数");
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Message.ERROR + POSITIVE_AMOUNT_ERROR);
            }
        }
    }

    private void delete() {
        if (args.length != 2) {
            sendParameterError();
        } else if (Gem.getPlugin().getGemExecutor().deleteData(args[1])) {
            sender.sendMessage(Message.INFO + args[1] + " 的宝石数据已删除");
        } else {
            sender.sendMessage(Message.ERROR + args[1] + " 没有可删除的宝石数据");
        }
    }

    private void check() {
        if (args.length != 2) {
            sendParameterError();
        } else {
            Integer gems = Gem.getPlugin().getGemExecutor().getGems(args[1]);
            sender.sendMessage(Message.INFO + args[1] + " 现在有 " + gems + " 宝石");
        }
    }

    private void total() {
        if (args.length != 2) {
            sendParameterError();
        } else {
            Integer gems = Gem.getPlugin().getGemExecutor().getTotalGems(args[1]);
            sender.sendMessage(Message.INFO + args[1] + " 累计 " + gems + " 宝石");
        }
    }

    private void take() {
        if (args.length != 3) {
            sendParameterError();
        } else {
            try {
                int gems = parsePositiveAmount(args[2]);
                if (Gem.getPlugin().getGemExecutor().takeGems(args[1], gems)) {
                    sender.sendMessage(Message.INFO + args[1] + " 减少 " + gems + " 宝石");
                } else {
                    sender.sendMessage(Message.ERROR + args[1] + " 宝石不足");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Message.ERROR + "宝石必须是整数");
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Message.ERROR + POSITIVE_AMOUNT_ERROR);
            }
        }
    }

    private void add() {
        if (args.length != 3) {
            sendParameterError();
        } else {
            try {
                int addGems = parsePositiveAmount(args[2]);
                Gem.getPlugin().getGemExecutor().addGems(args[1], addGems);
                int gems = Gem.getPlugin().getGemExecutor().getGems(args[1]);
                sender.sendMessage(Message.INFO + args[1] +
                        " 增加了 " + addGems + " 宝石, 现在有 " + gems + " 宝石");
                if (Bukkit.getPlayer(args[1]) != null) {
                    Bukkit.getPlayer(args[1]).sendMessage("收到 " + addGems + " 宝石, 现在有 " + gems + " 宝石");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Message.ERROR + "宝石必须是整数");
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Message.ERROR + POSITIVE_AMOUNT_ERROR);
            }
        }
    }

    private int parsePositiveAmount(String value) {
        int amount = Integer.parseInt(value);
        if (amount <= 0) {
            throw new IllegalArgumentException(POSITIVE_AMOUNT_ERROR);
        }
        return amount;
    }

    private void sendParameterError() {
        sender.sendMessage(Message.ERROR + "参数长度有误");
    }

}

