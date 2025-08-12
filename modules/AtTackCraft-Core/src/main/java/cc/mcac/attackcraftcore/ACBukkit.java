package cc.mcac.attackcraftcore;

import cc.mcac.attackcraftcore.Bukkit.TaoYuanProtection;
import cc.mcac.attackcraftcore.Bukkit.WorldProtection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ACBukkit extends JavaPlugin {

    public static ACBukkit plugin = null;


    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        loadPlaceholderAPI();
        loadCommandsOnEnable();
        loadWorldProtection();
        getLogger().info("启动成功");
    }

    @Override
    public void onDisable() {
    }

    private void loadPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("已接入PlaceholderAPI");
        }
    }

    private void loadCommandsOnEnable() {
        List<String> commands = getConfig().getStringList("bukkit.commands_on_enable");
        if (commands == null || commands.isEmpty()) {
            return;
        }
        getLogger().info("正在执行BukkitOnEnable指令");
        for (String command : commands) {
            getLogger().info("执行指令: " + command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private void loadWorldProtection() {
        if (getConfig().getString("server_id").equals("mcac")) {
            getLogger().info("当前服务器为mcac, 已启用桃源保护");
            Bukkit.getPluginManager().registerEvents(new TaoYuanProtection(), this);
        }
        List<String> worldList = getConfig().getStringList("bukkit.world_protection");
        if (worldList.isEmpty()) {
            return;
        }
        getLogger().info("已启用世界保护: " + worldList);
        Bukkit.getPluginManager().registerEvents(new WorldProtection(worldList), this);
    }
}
