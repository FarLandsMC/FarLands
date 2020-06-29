package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Chat;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandResetHome extends PlayerCommand {
    public CommandResetHome() {
        super(Rank.INITIATE, Category.HOMES, "Move a home to where you are standing.", "/resethome [homeName]",
                "resethome", "movehome", "movhome");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Is the sender a staff member and are they moving someone else's home
        boolean moveUnownedHome = Rank.getRank(sender).isStaff() && args.length > 1;
        OfflineFLPlayer flp = moveUnownedHome
                ? FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1])
                : FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (flp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        // Check to make sure the location is in a valid world
        Location location = sender.getLocation();
        if (!("world".equals(location.getWorld().getName()) || "world_nether".equals(location.getWorld().getName()))) {
            sendFormatted(sender, "&(red)You can only move homes to the overworld and nether. Reset cancelled");
            return true;
        }

        // Check for claims
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(location);
        if (!(flp.rank.isStaff() || flags == null || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.ACCESS, flags))) {
            sendFormatted(sender, "&(red)You do not have permission to move a home into this claim.");
            return true;
        }

        // Parse out the name
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (args[0].equals("home")) {
                sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/movhome home} by typing " +
                        "$(hovercmd,/movhome,{&(gray)Click to Run},&(dark_aqua)/movhome)!");
            }

            name = args[0];
        }

        // If the home already exists then move it
        if (flp.hasHome(name)) {
            flp.moveHome(name, location);
            sendFormatted(sender, "&(green)Moved home with name {&(aqua)%0} to your location.", name);
        }
        // If the home does not exist try to make a new one
        else {
            if (moveUnownedHome) {
                sendFormatted(sender, "&(red)%0 does not have a home with this name.", args[1]);
                return true;
            }

            // Create the home if it doesn't exist and the user has enough homes to set another
            if (flp.numHomes() >= flp.rank.getHomes()) {
                sendFormatted(sender, "&(red)This home does not exist and you do not have enough homes to set another.");
                return true;
            } else {
                if (args.length > 0 && (args[0].isEmpty() || args[0].matches("\\s+") || Chat.getMessageFilter().isProfane(args[0]))) {
                    sendFormatted(sender, "&(red)This home does not exist. Unable to create home with that name.");
                    return true;
                }

                flp.addHome(name, location);
                sendFormatted(sender, "&(green)This home does not exist, created a new home with name " +
                        "{&(aqua)%0} at your current location.", name);
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], FarLands.getDataHandler().getOfflineFLPlayer(sender).homes.stream().map(Home::getName))
                // Suggest player names for staff
                : (Rank.getRank(sender).isStaff() ? getOnlinePlayers(args[1], sender) : Collections.emptyList());
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/resethome <name> [player]" : getUsage()));
    }
}