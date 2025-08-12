package cc.mcac.attackcraftcore.Bukkit;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.List;

public class WorldProtection implements Listener {

    private final HashSet<String> worldList;

    public WorldProtection(List<String> worldList) {
        this.worldList = new HashSet<>(worldList);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        String worldName = e.getPlayer().getWorld().getName();
        if (worldList.contains(worldName)) {
            if (e.getPlayer().hasPermission("mcac.world_protection" + worldName)) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        String worldName = e.getPlayer().getWorld().getName();
        if (worldList.contains(worldName)) {
            if (e.getPlayer().hasPermission("mcac.world_protection" + worldName)) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent e) {
        String worldName = e.getPlayer().getWorld().getName();
        if (worldList.contains(worldName)) {
            if (e.getPlayer().hasPermission("mcac.world_protection" + worldName)) {
                return;
            }
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.WATER_BUCKET
                        || e.getPlayer().getInventory().getItemInMainHand().getType() == Material.LAVA_BUCKET
                        || e.getPlayer().getInventory().getItemInMainHand().getType() == Material.BUCKET) {
                    e.setCancelled(true);
                }
            }
        }

    }


}
