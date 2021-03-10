package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandStats extends Command {
    public CommandStats() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Show the stats of a player.", "/stats", "stats");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        final boolean isPersonal = args.length <= 0;
        final OfflineFLPlayer flp = isPersonal ? FarLands.getDataHandler().getOfflineFLPlayer(sender)
                : FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(flp.uuid);
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            flp.updateAll(false); // Make sure our stats are fresh
            sender.sendMessage(ChatColor.GREEN +
                    "Showing stats for " + ChatColor.GOLD + flp.username + ":" + ChatColor.GREEN + "\n" +
                    (flp.pronouns == null || flp.pronouns.toString() == null ? "" : "Pronouns: " + flp.pronouns.toString(false) + "\n") +
                    "Rank: " + flp.rank.getColor() + flp.rank.getName() + ChatColor.GREEN + "\n" +
                    "Time Played: " + TimeInterval.formatTime(flp.secondsPlayed * 1000L, false) + "\n" +
                    (isPersonal && sender instanceof Player && flp.amountDonated > 0 ? "Amount Donated: $" +
                            flp.amountDonated + "\n" : "") +
                    (flp.birthday != null ? "Birthday: " + flp.birthday.toFormattedString() + "\n" : "") +
                    "Deaths: " + offlinePlayer.getStatistic(Statistic.DEATHS) + "\n" +
                    "Votes this Month: " + flp.monthVotes + "\n" +
                    "Total Votes this Season: " + flp.totalSeasonVotes + "\n" +
                    "Total Votes All Time: " + flp.totalVotes
            );
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
