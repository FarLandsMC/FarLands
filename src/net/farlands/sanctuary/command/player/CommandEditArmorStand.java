package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class CommandEditArmorStand extends PlayerCommand {
    private final ItemStack editorBook;

    public CommandEditArmorStand() {
        super(Rank.SPONSOR, Category.UTILITY, "Open the vanilla tweaks armor stand editor book.",
                "/editarmorstand [marker|set] <on|off|value>", "editarmorstand", "editarmourstand");
        this.editorBook = FarLands.getFLConfig().armorStandBook.getStack();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // /editarmourstand enable <player> - Enables a player's `as_trigger`, `as_help`, and `if_invisible` scoreboard tags
        if(args.length > 0 && args[0].equalsIgnoreCase("enable") &&
                FarLands.getDataHandler().getOfflineFLPlayer(sender).rank.isStaff()){
            if(args.length > 1 && !args[1].isEmpty()){
                // enable the scoreboard triggers
                String[] scoreboardTriggers = {"as_trigger", "as_help", "if_invisible"};
                for (String trigger : scoreboardTriggers) {
                    Bukkit.dispatchCommand(sender, "scoreboard players enable " + trigger + " as_trigger");
                }
                Logging.broadcastStaff(ChatColor.RED + sender.getName() + ": " + ChatColor.GRAY + "/editarmorstand enable " + args[1]);
            }
            return true;

        }
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(sender.getLocation());
        if (flags != null && !flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.BUILD, flags)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to edit armor stands here.");
            return true;
        }

        if (args.length > 0){
            switch(args[0].toLowerCase()) {
                case "marker":
                    if (args.length == 1) {
                        sender.sendMessage(ChatColor.RED + "Please specify either \"on\" or \"off\"");
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
                        sender.sendMessage(ChatColor.GOLD + "Toggled marker tag " + (marker ? "on." : "off."));
                        stand.setInvisible(!stand.isInvisible());
                        FarLands.getScheduler().scheduleSyncDelayedTask(
                            () -> stand.setInvisible(!stand.isInvisible()),
                            20
                        );
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please stand near and look at the armor stand you on which you " +
                                "wish to toggle the marker tag.");
                    }
                    return true;
                // /editarmourstand set <value> -> /trigger as_trigger set <value>
                case "set":
                    if (args.length == 1) {
                        sender.sendMessage(ChatColor.RED + "Please specify a value.");
                        return true;
                    }
                    try {
                        Integer.parseInt(args[1]);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Invalid Value.");
                        return true;
                    }
                    sender.performCommand("trigger as_trigger set " + args[1]);
                    return true;
            }

        }

        sender.openBook(editorBook);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                List<String> subCommands = new ArrayList<>(Arrays.asList("marker", "set"));
                if(FarLands.getDataHandler().getOfflineFLPlayer(sender).rank.isStaff())
                    subCommands.add("enable");
                return TabCompleterBase.filterStartingWith(args[0], subCommands);
            case 2:
                List<String> values;
                switch(args[0].toLowerCase()){
                    case "marker":
                        values = Arrays.asList("on", "off");
                        break;
                    case "set":
                        values = Collections.singletonList("<value>");
                        break;
                    case "enable":
                        values = TabCompleterBase.getOnlinePlayers(args[1]);
                        break;
                    default:
                        values = Collections.emptyList();
                        break;
                }
                return values;
            default:
                return Collections.emptyList();
        }
    }
}
