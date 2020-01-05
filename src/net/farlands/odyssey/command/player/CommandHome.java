package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.util.FLUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHome extends PlayerCommand {
    public CommandHome() {
        super(Rank.INITIATE, "Go to a home that you have already set.", "/home [name=\"home\"]", "home");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        boolean flag = Rank.getRank(sender).isStaff() && args.length > 1; // Is the sender a staff member and are they going to someone else's home
        OfflineFLPlayer flp = flag ? FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1])
                : FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (!flag && args[0].equals("home")) {
                sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/home home} by typing " +
                        "$(hovercmd,/home,{&(gray)Click to Run},&(dark_aqua)/home)!");
            }
            name = args[0];
        }
        Location loc = flp.getHome(name);
        if(loc == null) {
            sender.sendMessage(ChatColor.RED + (flag ? args[1] + " does" : "You do") + " not have that home.");
            return false;
        }
        FLUtils.tpPlayer(sender, loc);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? FarLands.getDataHandler().getOfflineFLPlayer(sender).getHomes().stream().map(Home::getName)
                    .filter(home -> home.startsWith(args.length == 0 ? "" : args[0]))
                    .collect(Collectors.toList())
                : (Rank.getRank(sender).isStaff() ? getOnlinePlayers(args[1], sender) : Collections.emptyList()); // For staff
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/home <name> [player]" : getUsage()));
    }
}
