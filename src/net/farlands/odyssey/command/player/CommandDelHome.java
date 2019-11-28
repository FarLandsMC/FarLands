package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.FLPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandDelHome extends Command {
    public CommandDelHome() {
        super(Rank.INITIATE, "Delete a home you have already set.", "/delhome [name=\"home\"]", "delhome");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }
        boolean flag = Rank.getRank(sender).isStaff() && args.length > 1; // Whether or not we're deleting someone else's home
        FLPlayer flp = flag ? getFLPlayer(args[1]) : FarLands.getPDH().getFLPlayer(sender);
        if(flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        String name = args.length == 0 ? "home" : args[0];
        if(!flp.hasHome(name)) {
            sender.sendMessage(ChatColor.RED + (flag ? flp.getUsername() + " does" : "You do") + " not have that home.");
            return true;
        }
        flp.removeHome(name);
        FarLands.getDataHandler().getRADH().store(name, "delhome", sender.getName());
        sender.sendMessage(ChatColor.GREEN + "Removed home " + ChatColor.AQUA + name);
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
                FarLands.getDataHandler().getRADH().delete("delhome", sender.getName()), 300);
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

    @Override
    protected void showUsage(CommandSender sender) {
        if(Rank.getRank(sender).isStaff())
            sender.sendMessage("Usage: /delhome <name> [player]");
        else
            sender.sendMessage("Usage: /delhome <name>");
    }
}
