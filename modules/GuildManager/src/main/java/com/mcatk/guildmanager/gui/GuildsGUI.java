package com.mcatk.guildmanager.gui;

import com.mcatk.guildmanager.GuildManager;
import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.GuildBasicInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;

public class GuildsGUI implements Listener {

    private final Inventory gui;
    private ItemStack back;

    private HashMap<ItemStack, Guild> guildMap;

    public GuildsGUI() {
        Bukkit.getPluginManager().registerEvents(this, GuildManager.getPlugin());
        gui = getGuildsGui();
    }

    public void openGUI(Player player) {
        player.openInventory(gui);
    }

    public Inventory getGuildsGui() {
        Inventory gui = Bukkit.createInventory(null, 54, "§6公会列表");
        guildMap = new HashMap<>();
        for (Guild guild : GuildManager.getPlugin().getGuildService().getGuilds().values()) {
            int nextSlot = gui.firstEmpty();
            if (nextSlot == -1 || nextSlot >= 53) {
                break;
            }
            ItemStack button = getAnGuildButton(guild);
            guildMap.put(button, guild);
            gui.addItem(button);
        }
        gui.setItem(53, back = getQuitIcon());
        return gui;
    }

    private ItemStack getAnGuildButton(Guild guild) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l" + guild.getGuildName());
        ArrayList<String> des = new ArrayList<>();
        des.add("§b公会ID: §a" + guild.getId());
        des.add("§b会长: §a" + guild.getChairman());
        des.add("§b成员: §a" + GuildManager.getPlugin().getGuildService().getGuildMembers(guild.getId()).size() + "§7/§2" + GuildBasicInfo.getMaxPlayer(guild.getLevel()));
        des.add("§b高级成员: §a" + GuildBasicInfo.getMaxAdvancedPlayer(guild.getLevel()));
        des.add("§b等级: §a" + guild.getLevel());
        des.add("§b积分: §a" + guild.getPoints());
        des.add("§b资金: §a" + guild.getCash());
        des.add("");
        des.add("§e点击查看详情");
        meta.setLore(des);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getQuitIcon() {
        ItemStack icon = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName("§e返回");
        ArrayList<String> lore = new ArrayList<>();
        lore.add("§7点击返回主菜单");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(gui)) return;
        e.setCancelled(true);
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (clickedItem.equals(back)) {
            ((Player) e.getWhoClicked()).chat("/menu");
            return;
        }
        Guild guild = guildMap.get(clickedItem);
        if (guild != null) {
            new GuildGUI(guild).openGUI((Player) e.getWhoClicked());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(gui)) {
            HandlerList.unregisterAll(this);
        }
    }

}
