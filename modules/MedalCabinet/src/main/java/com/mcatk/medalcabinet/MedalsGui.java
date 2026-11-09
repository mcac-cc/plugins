package com.mcatk.medalcabinet;

import com.mcatk.medalcabinet.medal.Medal;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import java.util.List;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class MedalsGui implements Listener {

    private final Inventory gui;
    private final ItemStack back;
    private boolean updating;
    private final HashMap<ItemStack, Medal> iconMap = new HashMap<>();

    public MedalsGui(String playerID, List<Medal> medals) {
        gui = Bukkit.createInventory(null, 54, playerID + " §6的勋章墙");
        for (Medal medal : medals) {
            if (gui.firstEmpty() < 0 || gui.firstEmpty() >= 53) break;
            ItemStack icon = getIcon(medal);
            gui.addItem(icon);
            iconMap.put(icon, medal);
        }
        // back button
        back = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName("\u00a7e返回");
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77点击返回主菜单");
        meta.setLore(lore);
        back.setItemMeta(meta);
        gui.setItem(53, back);
        Bukkit.getPluginManager().registerEvents(this, MedalCabinet.getPlugin());
    }

    public void openGUI(Player player) {
        player.openInventory(gui);
    }

    private ItemStack getIcon(Medal medal) {
        Material material = resolveMaterial(medal);
        ItemStack icon = new ItemStack(material, 1);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(medal.getName());
        ArrayList<String> lore = new ArrayList<>(Arrays.asList(medal.getDescriptions().split("\n")));
        lore.add("§0" + medal.getId());
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private Material resolveMaterial(Medal medal) {
        String materialName = medal.getMaterial();
        Material material = materialName == null ? null : Material.getMaterial(materialName.toUpperCase(Locale.ROOT));
        if (material != null) {
            return material;
        }

        MedalCabinet.getPlugin().getLogger().warning(
                "Invalid material '" + materialName + "' for medal " + medal.getId() + ", falling back to PAPER"
        );
        return Material.PAPER;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(gui)) return;
        e.setCancelled(true);
        if (e.getRawSlot() < 0 || e.getRawSlot() >= gui.getSize()) return;
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.hasItemMeta() &&
            clickedItem.getItemMeta().hasDisplayName() &&
            clickedItem.getItemMeta().getDisplayName().endsWith("返回")) {
            ((Player) e.getWhoClicked()).chat("/menu");
            return;
        }

        Medal medal = iconMap.get(clickedItem);
        if (medal != null) {
            if (updating) return;
            updating = true;
            Player player = (Player) e.getWhoClicked();
            String name = player.getName();
            MedalCabinet.getPlugin().queryAsync(() -> SQLManager.getInstance().setMainMedal(name, medal.getId()), updated -> {
                updating = false;
                if (player.isOnline()) player.sendMessage(updated
                        ? "§e已将勋章 " + medal.getName() + " §e设为主勋章" : "§c勋章设置失败，请稍后重试");
                if (updated) {
                    MedalCabinet.getPlugin().clearTitle(name);
                    MedalCabinet.getPlugin().requestTitle(name);
                }
            }, error -> {
                updating = false;
                if (player.isOnline()) player.sendMessage("§c勋章设置失败，请稍后重试");
            });
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(gui)) HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().equals(gui) && event.getRawSlots().stream().anyMatch(slot -> slot < gui.getSize())) {
            event.setCancelled(true);
        }
    }
}
