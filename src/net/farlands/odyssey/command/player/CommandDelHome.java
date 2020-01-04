package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandDelHome extends Command {
    public CommandDelHome() {
        super(Rank.INITIATE, "Delete a home you have already set.", "/delhome [name=\"home\"]", "delhome");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }
        boolean flag = Rank.getRank(sender).isStaff() && args.length > 1; // Whether or not we're deleting someone else's home
        OfflineFLPlayer flp = flag ? FarLands.getDataHandler().getOfflineFLPlayer(args[0])
                : FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (args[0].equals("home"))
                sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/delhome home} by typing " +
                        "$(hovercmd,/delhome,{&(gray)Click to Run},&(dark_aqua)/delhome)!");
            name = args[0];
        }
        if (!flp.hasHome(name)) {
            sender.sendMessage(ChatColor.RED + (flag ? flp.username + " does" : "You do") + " not have that home.");
            return false;
        }
        flp.removeHome(name);
        if (!flag && sender instanceof Player)
            FarLands.getDataHandler().getSession((Player)sender).lastDeletedHomeName.setValue(args[0], 300L, null);
        sender.sendMessage(ChatColor.GREEN + "Removed home " + ChatColor.AQUA + name);
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
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/delhome <name> [player]" : getUsage()));
    }
}
