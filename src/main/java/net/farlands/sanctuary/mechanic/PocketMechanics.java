package net.farlands.sanctuary.mechanic;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;

public class PocketMechanics extends Mechanic {

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (Worlds.POCKET.matches(event.getWorld())) { // Cancel all portal creation in the pocket world
            if (event.getEntity() != null) {
                event.getEntity().sendMessage(ComponentColor.red("You cannot open a portal in the pocket world."));
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!Worlds.POCKET.matches(event.getPlayer().getWorld())) return;

        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();

        if ( // Prevent Ender Portal
            event.getAction().isRightClick()
            && item != null
            && item.getType() == Material.ENDER_EYE
            && block != null
            && block.getType() == Material.END_PORTAL_FRAME
        ) {
            event.getPlayer().sendMessage(ComponentColor.red("You cannot open a portal in the pocket world."));
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (Worlds.POCKET.matches(event.getPlayer().getWorld()) && event.getCause() == PlayerSetSpawnEvent.Cause.BED) {
            event.getPlayer().sendMessage(ComponentColor.red("Your spawn point was not set because you're in the Pocket world."));
            event.setCancelled(true);
        }
    }

}
