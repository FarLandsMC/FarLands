package net.farlands.odyssey.mechanic.region;

import net.farlands.odyssey.command.FLCommandEvent;
import net.farlands.odyssey.command.player.CommandSetHome;
import net.farlands.odyssey.mechanic.Mechanic;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.FLUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import static net.farlands.odyssey.util.FLUtils.region;

public class Spawn extends Mechanic {

    private static final Pair<Location, Location> END_PORTAL = region(-116, 130, -76, -109, 137, -67);

    public Spawn() {
    }

    @EventHandler
    public void onFLCommand(FLCommandEvent event) {
        if (CommandSetHome.class.equals(event.getCommand()) && event.getSender() instanceof Player &&
                FLUtils.isWithin(((Player) event.getSender()).getLocation(), END_PORTAL)) {
            event.getSender().sendMessage(ChatColor.RED + "You cannot set a home here.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        checkSpawnPortal(event.getFrom(), event.getTo(), event);
    }

    private void checkSpawnPortal(Location from, Location to, PlayerMoveEvent event) {
        if (!FLUtils.isWithin(from, END_PORTAL) && FLUtils.isWithin(to, END_PORTAL)) {
            Player player = event.getPlayer();
            ItemStack stack = player.getInventory().getItemInMainHand();
            if (Material.DIAMOND.equals(stack.getType()) && stack.getAmount() >= 5) {
                stack.setAmount(stack.getAmount() - 5);
                player.getInventory().setItemInMainHand(stack.getAmount() == 0 ? null : stack);
                player.sendMessage(ChatColor.GREEN + "Payment accepted. You may now use the portal.");
            } else {
                player.sendMessage(ChatColor.RED + "To enter this room, you must be holding at least 5 diamonds in your main hand. " +
                        "5 diamonds will be removed from your hand upon entering.");
                event.setCancelled(true);
            }
        }
    }
}