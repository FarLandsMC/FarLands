package net.farlands.sanctuary.mechanic.region;

import com.kicas.rp.data.Region;

import net.farlands.sanctuary.command.FLCommandEvent;
import net.farlands.sanctuary.command.player.CommandSetHome;
import net.farlands.sanctuary.mechanic.Mechanic;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Handles FarLands Spawn.
 */
public class Spawn extends Mechanic {

    private static final Region END_PORTAL = new Region(
            new Location(Bukkit.getWorld("world"), 111, 78, -133),
            new Location(Bukkit.getWorld("world"), 111, 81, -131)
    );

    public Spawn() {

    }

    @EventHandler
    public void onFLCommand(FLCommandEvent event) {
        if (CommandSetHome.class.equals(event.getCommand()) && event.getSender() instanceof Player &&
                END_PORTAL.contains(((Player) event.getSender()).getLocation())) {
            event.getSender().sendMessage(ChatColor.RED + "You cannot set a home here.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        checkSpawnPortal(event.getFrom(), event.getTo(), event);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        checkSpawnPortal(event.getFrom(), event.getTo(), event);
    }

    private void checkSpawnPortal(Location from, Location to, PlayerMoveEvent event) {
        if (!END_PORTAL.contains(from) && END_PORTAL.contains(to)) {
            Player player = event.getPlayer();
            if (!AutumnEvent.isActive()) {
                player.sendMessage(ChatColor.RED + "There are no events running.");
                event.setCancelled(true);
                // yeet them in the other direction to reduce chat spam
                from.setYaw(from.getYaw() + 180);
                player.teleport(from);
                return;
            }
            player.teleport(AutumnEvent.getSpawn());
        }
    }
}