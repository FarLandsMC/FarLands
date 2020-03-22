package net.farlands.odyssey.mechanic.region;

import com.kicas.rp.data.Region;

import net.farlands.odyssey.command.FLCommandEvent;
import net.farlands.odyssey.command.player.CommandSetHome;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Mechanic;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class Spawn extends Mechanic {

    private static final int END_PORTAL_COST = 5;
    private static final Region END_PORTAL = new Region(
            new Location(Bukkit.getWorld("world"), -116, 130, -76),
            new Location(Bukkit.getWorld("world"), -109, 137, -67));

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
            if (player.getGameMode() != GameMode.SURVIVAL && Rank.getRank(player).isStaff())
                return;
            ItemStack stack = player.getInventory().getItemInMainHand();
            if (Material.DIAMOND == stack.getType() && stack.getAmount() >= END_PORTAL_COST) {
                stack.setAmount(stack.getAmount() - END_PORTAL_COST);
                player.getInventory().setItemInMainHand(stack.getAmount() == 0 ? null : stack);
                player.sendMessage(ChatColor.GREEN + "Payment accepted. You may now use the portal.");
            } else {
                player.sendMessage(ChatColor.RED + "To enter this room, you must be holding at least 5 diamonds in your main hand. " +
                        "5 diamonds will be removed from your hand upon entering.");
                event.setCancelled(true);
                // yeet them in the other direction to reduce chat spam
                from.setYaw(from.getYaw() + 180);
                player.teleport(from);
            }
        }
    }
}