package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.mechanic.AFK;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSetRank extends Command {
    public CommandSetRank() {
        super(Rank.BUILDER, "Sets the rank of a player.", "/setrank <player> <rank>", "setrank");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length < 2)
            return false;
        FLPlayer flp = getOnlineOrOfflinePlayer(args[0]);
        if(flp == null) {
            sender.sendMessage(ChatColor.RED + "Could not find player: " + args[0]);
            return true;
        }
        Rank rank = Utils.safeValueOf(Rank::valueOf, args[1].toUpperCase());
        if(rank == null) {
            sender.sendMessage(ChatColor.RED + "Invalid rank: " + args[1]);
            return true;
        }
        // You cannot modify someone of an equal rank, and you cannot set someone to a higher rank than yours
        if((flp.getRank().specialCompareTo(Rank.getRank(sender)) >= 0 || rank.specialCompareTo(Rank.getRank(sender)) > 0) &&
                !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set " + ChatColor.WHITE + args[0] +
                    ChatColor.RED + " to rank " + ChatColor.WHITE + rank.toString());
            return true;
        }
        // Manage all the toggles and stuff that will change with rank
        if(!flp.getRank().isStaff() && rank.isStaff())
            FarLands.getDataHandler().getRADH().removeCooldown("afk", flp.getUuid().toString());
        else if(flp.getRank().isStaff() && !rank.isStaff() && flp.isOnline())
            AFK.setAFKCooldown(flp.getOnlinePlayer());
        flp.setRank(rank);
        sender.sendMessage(ChatColor.GREEN + "Updated " + ChatColor.AQUA + args[0] + "\'s" + ChatColor.GREEN + " rank to " + rank.getColor() + rank.toString());
        Player player = flp.getOnlinePlayer();
        if(player != null) // Notify the player if they're online
            player.sendMessage(ChatColor.GREEN + "Your rank has been updated to " + rank.getColor() + rank.toString());
        FarLands.getPDH().saveFLPlayer(flp);
        // Notify discord
        FarLands.getDiscordHandler().sendMessageRaw("output", Chat.applyDiscordFilters(sender.getName()) +
                " has updated " + Chat.applyDiscordFilters(flp.getUsername()) + "\'s rank to `" + rank.getSymbol() +
                "`.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch(args.length) {
            case 0:
            case 1:
                return getOnlinePlayers(args.length == 0 ? "" : args[0]);
            case 2:
                return Arrays.stream(Rank.VALUES).map(Rank::toString)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }
}
