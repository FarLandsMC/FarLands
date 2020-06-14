package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandHome extends PlayerCommand {
    public CommandHome() {
        super(Rank.INITIATE, "Go to a home that you have already set.", "/home [name=home]", "home");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Is the sender a staff member and are they going to someone else's home
        boolean gotoUnownedHome = Rank.getRank(sender).isStaff() && args.length > 1;

        // Get the player who owns the home
        OfflineFLPlayer flp = gotoUnownedHome ? FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1])
                : FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        // Get the home name
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (!gotoUnownedHome && args[0].equals("home")) {
                sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/home home} by typing " +
                        "$(hovercmd,/home,{&(gray)Click to Run},&(dark_aqua)/home)!");
            }
            name = args[0];
        }

        // Make sure the home exists
        Location loc = flp.getHome(name);
        if(loc == null) {
            sendFormatted(sender, "&(red)%0 not have a home named \"%1\"",
                    gotoUnownedHome ? flp.username + " does" : "You do", name);
            return false;
        }

        FLUtils.tpPlayer(sender, loc);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], FarLands.getDataHandler().getOfflineFLPlayer(sender).homes.stream().map(Home::getName))
                : (Rank.getRank(sender).isStaff() ? getOnlinePlayers(args[1], sender) : Collections.emptyList()); // For staff
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/home <name> [player]" : getUsage()));
    }
}
