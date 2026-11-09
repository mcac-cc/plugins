package com.mcatk.gemshop;

import org.bukkit.Bukkit;

public class ServerCmd {
    public static boolean sendConsoleCmd(String cmd) {
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)) {
            throw new IllegalStateException("Command was not acknowledged; reconcile delivery before retrying");
        }
        return true;
    }
    
    public static boolean addPermission(String player, String permission) {
        return sendConsoleCmd("lp user " + player + " permission set " + permission + " true");
    }
    
    public static void removePermission(String player, String permission) {
        sendConsoleCmd("lp user " + player + " permission clear " + permission);
    }
    
}
