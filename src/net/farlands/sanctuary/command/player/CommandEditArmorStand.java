package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandEditArmorStand extends PlayerCommand {
    private final ItemStack editorBook;

    public CommandEditArmorStand() {
        super(Rank.SPONSOR, Category.UTILITY, "Open the vanilla tweaks armor stand editor book.",
                "/editarmorstand [marker] <on|off>", "editarmorstand", "editarmourstand");
        this.editorBook = FarLands.getFLConfig().armorStandBook.getStack();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(sender.getLocation());
        if (flags != null && !flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.BUILD, flags)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to edit armor stands here.");
            return true;
        }

        if (args.length > 0 && "marker".equalsIgnoreCase(args[0])) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Please specify either \"on\" or \"off\"");
                return true;
            }
            boolean marker = "on".equalsIgnoreCase(args[1]);

            Location location = sender.getLocation();
            double yaw = location.getYaw();
            double pitch = location.getPitch();
            double vx = -Math.sin(yaw   * FLUtils.DEGREES_TO_RADIANS) * Math.cos(pitch * FLUtils.DEGREES_TO_RADIANS);
            double vy = -Math.sin(pitch * FLUtils.DEGREES_TO_RADIANS);
            double vz =  Math.cos(yaw   * FLUtils.DEGREES_TO_RADIANS) * Math.cos(pitch * FLUtils.DEGREES_TO_RADIANS);
            Vector velocity = (new Vector(vx, vy, vz)).normalize();

            double maxDot = -Double.MAX_VALUE;
            Collection<Entity> entities = sender.getWorld().getNearbyEntities(
                    sender.getLocation(),
                    2.5, 2.5, 2.5,
                    entity -> entity.getType() == EntityType.ARMOR_STAND
            );
            Entity selected = null;
            for (Entity entity : entities) {
                Vector direction = entity.getLocation().toVector().subtract(location.toVector()).normalize();
                double dot = velocity.dot(direction);
                if (dot > maxDot && dot > 0) {
                    maxDot = dot;
                    selected = entity;
                }
            }

            if (selected != null) {
                ((ArmorStand) selected).setMarker(marker);
                sender.sendMessage(ChatColor.GOLD + "Toggled marker tag " + (marker ? "on." : "off."));
            } else {
                sender.sendMessage(ChatColor.RED + "Please stand near and look at the armor stand you on which you " +
                        "wish to toggle the marker tag.");
            }

            return true;
        }

        sender.openBook(editorBook);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return Collections.singletonList("marker");
            case 2:
                return Arrays.asList("on", "off");
            default:
                return Collections.emptyList();
        }
    }
}
