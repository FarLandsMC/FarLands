package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandStats extends Command {
    public CommandStats() {
        super(Rank.INITIATE, "Show the stats of a player.", "/stats", "stats");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        final boolean isPersonal = args.length <= 0;
        final OfflineFLPlayer flp = isPersonal ? FarLands.getDataHandler().getOfflineFLPlayer(sender)
                : FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if(flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            flp.update(); // Make sure our stats are fresh
            sender.sendMessage(
                ChatColor.GREEN + "Showing stats for " + ChatColor.GOLD + flp.username + ":\n" +
                ChatColor.GREEN + "Rank: " + flp.rank.getColor() + flp.rank.getName() + ChatColor.GREEN + '\n' +
                "Time Played: " + TimeInterval.formatTime(flp.secondsPlayed * 1000L, false) + '\n' +
                (isPersonal && sender instanceof Player && flp.amountDonated > 0 ? "Amount Donated: $" +
                        flp.amountDonated + "\n" : "") +
                "Votes this Month: " + flp.monthVotes + '\n' +
                "Total Votes: " + flp.totalVotes
            );
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
