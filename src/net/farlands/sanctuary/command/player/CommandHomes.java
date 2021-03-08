package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.Command;

import net.farlands.sanctuary.util.Paginate;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandHomes extends Command {
    public CommandHomes() {
        super(Rank.INITIATE, Category.HOMES, "List your homes.", "/homes", "homes");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if ((sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) && args.length == 0) {
            sendFormatted(sender, "&(red)You must be in-game to use this command.");
            return true;
        }

        // Someone else's home (staff)
        if (Rank.getRank(sender).isStaff() && args.length > 0 && !NumberUtils.isNumber(args[0]) && args[0].length() > 2) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
            if (flp == null) {
                sendFormatted(sender, "&(red)Player not found.");
                return true;
            }

            if (flp.homes.isEmpty()) {
                sendFormatted(sender, "&(green)This player does not have any homes.");
                return true;
            }

            List<String> lines = new ArrayList<>();
            // Build the list of homes to paginate
            flp.homes.forEach(home -> {
                Location location = home.getLocation();
                String line = "&(gold)$(hovercmd,/home " + home.getName() + " " + flp.username + ",Go to home {&(aqua)"
                        + home.getName() + "}," + home.getName() + ": {&(aqua)" + location.getBlockX()
                        + " " + location.getBlockY() + " " + location.getBlockZ() + "})";
                lines.add(line);
            });

            String header = flp.username + "'s " + flp.homes.size() + " home" + (flp.homes.size() == 1 ? "" : "s");
            Paginate paginate = new Paginate(lines, header, 8, "homes " + flp.username);
            String toSend = args.length == 1 ? paginate.getPage(1) : paginate.getPage(Integer.parseInt(args[1]));

            if (toSend == null) {
                sendFormatted(sender, "&(red)Invalid page number. Must be between 1 and %0.",
                        paginate.getMaxPage()); return true;
            }

            sendFormatted(sender, toSend);
        }
        // The sender's homes
        else {
            List<Home> homes = FarLands.getDataHandler().getOfflineFLPlayer(sender).homes;
            if (homes.isEmpty()) {
                sendFormatted(sender, "&(green)You don't have any homes! Set one with &(aqua)/sethome");
                return true;
            }
            List<String> lines = new ArrayList<>();
            // Build the list of homes to paginate
            homes.forEach(home -> {
                Location location = home.getLocation();
                String line = "$(hovercmd,/home " + home.getName() + ",{&(gold)Go to home " +
                        "{&(" + (home.getLocation().getWorld().getEnvironment() == World.Environment.NORMAL
                        ? "green" : "red") + ")" + home.getName() + "}},{{&(" + (home.getLocation().getWorld()
                        .getEnvironment() == World.Environment.NORMAL ? "green" : "red") + ")" + home.getName()
                        + ":} {&(aqua)" + location.getBlockX() + " " + location.getBlockY() +
                        " " + location.getBlockZ() + "}})";
                lines.add(line);
            });

            String header = "Your " + homes.size() + " home" + (homes.size() == 1 ? "" : "s");
            Paginate paginate = new Paginate(lines, header, 8, "homes");
            String toSend = args.length == 0 || !NumberUtils.isNumber(args[0]) ? paginate.getPage(1) : paginate.getPage(Integer.parseInt(args[0]));

            if (toSend == null) {
                sendFormatted(sender, "&(red)Invalid page number. Must be between 1 and %0.",
                        paginate.getMaxPage()); return true;
            }

            sendFormatted(sender, toSend);
        }
        return true;
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/homes [player]" : getUsage()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && Rank.getRank(sender).isStaff()
                ? getOnlinePlayers(args[0], sender)
                : Collections.emptyList();
    }
}