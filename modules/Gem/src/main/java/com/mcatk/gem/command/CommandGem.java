package com.mcatk.gem.command;

import com.mcatk.gem.Gem;
import com.mcatk.gem.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandGem implements CommandExecutor {

    private void printHelp(CommandSender sender) {
        sender.sendMessage("/gem set <player> <gems> 设置宝石");
        sender.sendMessage("/gem delete <player> 删除全部数据");
        sender.sendMessage("/gem check <player>  查看宝石");
        sender.sendMessage("/gem add <player> <gems> 增加宝石");
        sender.sendMessage("/gem take <player> <gems> 减少宝石");
        sender.sendMessage("/gem total <player> 累计宝石");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            // [Bolt] Execute database query asynchronously to avoid blocking the main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    int gems = Gem.getPlugin().getGemExecutor().getGems(sender.getName());
                    // Sync back to main thread for sending message
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(Message.INFO + "你的宝石：" + gems);
                        }
                    }.runTask(Gem.getPlugin());
                }
            }.runTaskAsynchronously(Gem.getPlugin());
            return true;
        }
        if (args.length == 0) {
            printHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "set":
                set(sender, args);
                break;
            case "check":
                check(sender, args);
                break;
            case "add":
                add(sender, args);
                break;
            case "take":
                take(sender, args);
                break;
            case "total":
                total(sender, args);
                break;
            default:
        }
        return true;
    }

    private void set(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendParameterError(sender);
        } else {
            // [Bolt] Execute database update asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        Gem.getPlugin().getGemExecutor().setGems(args[1], Integer.parseInt(args[2]));
                        // Sync back to main thread for message
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage(Message.INFO + args[1] + " 的宝石设置为： " + args[2]);
                            }
                        }.runTask(Gem.getPlugin());
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Message.ERROR + "宝石必须是整数");
                    }
                }
            }.runTaskAsynchronously(Gem.getPlugin());
        }
    }

    private void check(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sendParameterError(sender);
        } else {
            // [Bolt] Execute database query asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    Integer gems = Gem.getPlugin().getGemExecutor().getGems(args[1]);
                    // Sync back to main thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(Message.INFO + args[1] + " 现在有 " + gems + " 宝石");
                        }
                    }.runTask(Gem.getPlugin());
                }
            }.runTaskAsynchronously(Gem.getPlugin());
        }
    }

    private void total(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sendParameterError(sender);
        } else {
            // [Bolt] Execute database query asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    Integer gems = Gem.getPlugin().getGemExecutor().getTotalGems(args[1]);
                    // Sync back to main thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(Message.INFO + args[1] + " 累计 " + gems + " 宝石");
                        }
                    }.runTask(Gem.getPlugin());
                }
            }.runTaskAsynchronously(Gem.getPlugin());
        }
    }

    private void take(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendParameterError(sender);
        } else {
            // [Bolt] Execute database update asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        int gems = Integer.parseInt(args[2]);
                        boolean success = Gem.getPlugin().getGemExecutor().takeGems(args[1], gems);
                        // Sync back to main thread
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (success) {
                                    sender.sendMessage(Message.INFO + args[1] + " 减少 " + gems + " 宝石");
                                } else {
                                    sender.sendMessage(Message.ERROR + args[1] + " 宝石不足");
                                }
                            }
                        }.runTask(Gem.getPlugin());
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Message.ERROR + "宝石必须是整数");
                    }
                }
            }.runTaskAsynchronously(Gem.getPlugin());
        }
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendParameterError(sender);
        } else {
            // [Bolt] Execute database update asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        int addGems = Integer.parseInt(args[2]);
                        Gem.getPlugin().getGemExecutor().addGems(args[1], addGems);
                        int gems = Gem.getPlugin().getGemExecutor().getGems(args[1]);

                        // Sync back to main thread for Bukkit API usage (getPlayer) and messaging
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage(Message.INFO + args[1] +
                                        " 增加了 " + addGems + " 宝石, 现在有 " + gems + " 宝石");

                                org.bukkit.entity.Player target = Bukkit.getPlayer(args[1]);
                                if (target != null) {
                                    target.sendMessage("收到 " + addGems + " 宝石, 现在有 " + gems + " 宝石");
                                }
                            }
                        }.runTask(Gem.getPlugin());

                    } catch (NumberFormatException e) {
                        sender.sendMessage(Message.ERROR + "宝石必须是整数");
                    }
                }
            }.runTaskAsynchronously(Gem.getPlugin());
        }
    }

    private void sendParameterError(CommandSender sender) {
        sender.sendMessage(Message.ERROR + "参数长度有误");
    }

}
