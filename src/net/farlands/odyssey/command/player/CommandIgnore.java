package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Collections;
import java.util.List;

public class CommandIgnore extends Command {
    public CommandIgnore() {
        super(Rank.INITIATE, "Ignores a player so that you do not see any of their messages.", "/ignore <player>", true, "ignore", "unignore");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }
        if(args.length == 1)
            return false;
        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(sender);
        OfflineFLPlayer ignored = getOnlineOrOfflinePlayer(args[1]);
        if(ignored == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if(flp.getUuid().equals(ignored.getUuid())) {
            sender.sendMessage(ChatColor.RED + "You cannot ignore or unignore yourself.");
            return true;
        }
        if("ignore".equals(args[0])) {
            if(ignored.getRank().isStaff()) {
                sender.sendMessage(ChatColor.RED + "You cannot ignore a staff member.");
                return true;
            }
            if(!flp.setIgnoring(ignored.getUuid(), true)) {
                sender.sendMessage(ChatColor.RED + "You are already ignoring this player.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "You are now ignoring " + ChatColor.AQUA + args[1]);
        }else if("unignore".equals(args[0])) {
            if(!flp.setIgnoring(ignored.getUuid(), false)) {
                sender.sendMessage(ChatColor.RED + "You were not ignoring this player.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "You are no longer ignoring " + ChatColor.AQUA + args[1]);
        }
        if(sender instanceof DiscordSender)
            FarLands.getPDH().saveFLPlayer(flp);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }
}
