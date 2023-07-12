package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class CommandEditArmorStand extends PlayerCommand {

    public CommandEditArmorStand() {
        super(
            CommandData.withRank(
                    "editarmorstand",
                    "Open the vanilla tweaks armor stand editor book.",
                    "/editarmorstand [marker|set] <on|off|values>",
                    Rank.SPONSOR
                )
                .category(Category.UTILITY)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // /editarmourstand enable <player> - Enables a player's `as_trigger`, `as_help`, and `if_invisible` scoreboard tags
        if (args.length > 0 && args[0].equalsIgnoreCase("enable") &&
            FarLands.getDataHandler().getOfflineFLPlayer(sender).rank.isStaff()) {
            if (args.length > 1 && !args[1].isEmpty()) {
                // enable the scoreboard triggers
                String[] scoreboardTriggers = { "as_trigger", "as_help", "if_invisible" };
                for (String trigger : scoreboardTriggers) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players enable " + trigger + " as_trigger");
                }
                FarLands.getCommandHandler().logCommand(sender, "/editarmorstand enable " + args[1], null);
            }
            return true;

        }
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(sender.getLocation());
        if (flags != null && !flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.BUILD, flags)) {
            error(sender, "You do not have permission to edit armor stands here.");
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "marker":
                    if (args.length == 1) {
                        error(sender, "Please specify either \"on\" or \"off\".");
                        return true;
                    }
                    boolean marker = "on".equalsIgnoreCase(args[1]);

                    Location location = sender.getLocation();
                    double yaw = location.getYaw();
                    double pitch = location.getPitch();
                    double vx = -Math.sin(yaw * FLUtils.DEGREES_TO_RADIANS) * Math.cos(pitch * FLUtils.DEGREES_TO_RADIANS);
                    double vy = -Math.sin(pitch * FLUtils.DEGREES_TO_RADIANS);
                    double vz = Math.cos(yaw * FLUtils.DEGREES_TO_RADIANS) * Math.cos(pitch * FLUtils.DEGREES_TO_RADIANS);
                    Vector velocity = (new Vector(vx, vy, vz)).normalize();

                    double maxDot = -Double.MAX_VALUE;
                    Collection<Entity> entities = sender.getWorld().getNearbyEntities(
                        sender.getLocation(),
                        2.5, 2.5, 2.5,
                        entity -> entity.getType() == EntityType.ARMOR_STAND
                    );
                    Entity selected = null;
                    for (Entity entity : entities) {
                        // if the armour stand has the scoreboard tag for a chest shop, don't select it.
                        if (entity.getScoreboardTags().contains("chestShopDisplay")) {
                            continue;
                        }
                        Vector direction = entity.getLocation().toVector().subtract(location.toVector()).normalize();
                        double dot = velocity.dot(direction);
                        if (dot > maxDot && dot > 0) {
                            maxDot = dot;
                            selected = entity;
                        }
                    }

                    if (selected != null) {
                        ArmorStand stand = (ArmorStand) selected;
                        stand.setMarker(marker);
                        info(sender, "Toggled marker tag {}.", marker ? "on" : "off");
                        stand.setInvisible(!stand.isInvisible());
                        FarLands.getScheduler().scheduleSyncDelayedTask(
                            () -> stand.setInvisible(!stand.isInvisible()),
                            20
                        );
                    } else {
                        error(sender, "Please stand near and look at the armor stand you on which you wish to toggle the marker tag.");
                    }
                    return true;
                // /editarmourstand set <value> -> /trigger as_trigger set <value>
                case "set":
                    if (args.length == 1) {
                        error(sender, "Please specify a value.");
                        return true;
                    }

                    for (int i = 1; i < args.length; ++i) { // Parse the numbers before doing anything so we can error without changing stuff
                        try {
                            Integer.parseInt(args[i]);
                        } catch (Exception e) {
                            error(sender, "Invalid value: \"{}\"", args[i]);
                            return true;
                        }
                    }

                    for (int i = 1; i < args.length; ++i) { // then apply the codes
                        sender.performCommand("trigger as_trigger set " + args[i]);
                    }
                    return true;
            }

        }

        ItemStack editorBook = FarLands.getDataHandler().getItem("armorStandBook");
        if (editorBook == null) {
            error(sender, "Armor Stand Book not set! Please contact a staff member and notify them of this problem.");
            return true;
        }
        sender.openBook(editorBook);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return switch (args.length) {
            case 1 -> {
                List<String> subCommands = new ArrayList<>(Arrays.asList("marker", "set"));
                if (FarLands.getDataHandler().getOfflineFLPlayer(sender).rank.isStaff()) {
                    subCommands.add("enable");
                }
                yield TabCompleterBase.filterStartingWith(args[0], subCommands);
            }
            case 2 -> switch (args[0].toLowerCase()) {
                case "marker" -> Arrays.asList("on", "off");
                case "set" -> Collections.singletonList("<value>");
                case "enable" -> TabCompleterBase.getOnlinePlayers(args[1]);
                default -> Collections.emptyList();
            };
            default -> Collections.emptyList();
        };
    }
}
