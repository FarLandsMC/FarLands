package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.util.Utils;
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
        FLPlayer flp = flag ? getFLPlayer(args[1]) : FarLands.getPDH().getFLPlayer(sender);
        if(flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        Location loc = flp.getHome(args.length == 0 ? "home" : args[0]);
        if(loc == null) {
            sender.sendMessage(ChatColor.RED + (flag ? args[1] + " does" : "You do") + " not have that home.");
            return true;
        }
        Utils.tpPlayer(sender, loc);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? FarLands.getPDH().getFLPlayer(sender).getHomes().stream().map(Home::getName)
                    .filter(home -> home.startsWith(args.length == 0 ? "" : args[0]))
                    .collect(Collectors.toList())
                : (Rank.getRank(sender).isStaff() ? getOnlinePlayers(args[1]) : Collections.emptyList()); // For staff
    }
}
