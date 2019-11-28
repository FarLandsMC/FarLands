package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandSmite extends Command {
    public CommandSmite() {
        super(Rank.ADMIN, "Smite an inferior peasant.", "/smite <peasant>", "smite");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Please specify the peasant you wish to smite.");
            return true;
        }
        Player player = getPlayer(args[0]);
        if(player == null) {
            sender.sendMessage(ChatColor.RED + "This peasant does not exist.");
            return true;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at " + player.getName() + " run summon lightning_bolt ~ ~ ~");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0]) : Collections.emptyList();
    }
}
