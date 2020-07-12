package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
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
            flp.update(); // Make sure our stats are fresh
            sendFormatted(sender,
                    "&(green)Showing stats for {&(gold)%0:}\n" +
                            "Rank: {%1}\n" +
                            "Time Played: %2\n" +
                            (isPersonal && sender instanceof Player && flp.amountDonated > 0 ? "Amount Donated: \\$" +
                                    flp.amountDonated + "\n" : "") +
                            "Deaths: %3\n" +
                            "Votes this Month: %4\n" +
                            "Total Votes this Season: %5\n" +
                            "Total Votes All Time: %6",
                    flp.username,
                    "&(" + flp.rank.getColor().name().toLowerCase() + ")" + flp.rank.getName(),
                    TimeInterval.formatTime(flp.secondsPlayed * 1000L, false),
                    offlinePlayer.getStatistic(Statistic.DEATHS),
                    flp.monthVotes,
                    flp.totalSeasonVotes,
                    flp.totalVotes
            );
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
