package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.kicas.rp.util.TextUtils;
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
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            flp.updateAll(false); // Make sure our stats are fresh
            sender.sendMessage(playerInfo(flp, isPersonal && sender instanceof Player));
        });
        return true;
    }

    /**
     * Get formatted player info from an OfflineFLPlayer.
     * <br>
     * Current Display:
     * <pre>
     * "(username)'s stats"
     * Nickname (if set)
     * Pronouns (if set)
     * Rank
     * Time Played
     * Amount Donated (if `showDonated` and >0)
     * Deaths
     * Birthday (if set)
     * Votes this Month
     * Total Votes this season
     * Total Votes All Time
     * </pre>
     *
     * @param flp         player to get data from
     * @param showDonated Whether to show amount of money donated
     * @return the properly formatted text
     */
    public static String playerInfo(OfflineFLPlayer flp, boolean showDonated) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(flp.uuid);
        Rank displayedRank = flp.getDisplayRank();

        return (displayedRank.compareTo(Rank.SCHOLAR) > 0 ? displayedRank.getColor() : "") + flp.username + ChatColor.RESET +
            ChatColor.GOLD + "'s Stats: " + ChatColor.GOLD + "\n" +
            statLine("Nickname",
                flp.nickname,
                flp.nickname != null && !flp.nickname.isEmpty()
            ) +
            statLine("Pronouns",
                flp.pronouns.toString(false),
                flp.pronouns != null && flp.pronouns.toString() != null
            ) +
            statLine("Rank",
                flp.rank.getColor() + (flp.rank.isStaff() ? ChatColor.BOLD + "" : "") + flp.rank.getName()
            ) +
            statLine("Time Played",
                TimeInterval.formatTime(flp.secondsPlayed * 1000L, false)
            ) +
            statLine("Amount Donated",
                "$" + flp.amountDonated,
                showDonated && flp.amountDonated > 0
            ) +
            statLine("Deaths",
                offlinePlayer.getStatistic(Statistic.DEATHS)
            ) +
            statLine("Birthday",
                flp.birthday.toFormattedString(),
                flp.birthday != null
            ) +
            statLine("Votes this Month",
                flp.monthVotes
            ) +
            statLine("Total Votes this Season",
                flp.totalSeasonVotes
            ) +
            statLine("Total Votes All Time",
                flp.totalVotes, false, true
            );
    }

    /**
     * Ease of use for creating the stats
     * @param key The name of the value
     * @param value The value of the line
     * @param newline Should it have a newline at the end?
     * @param showWhen Should it show up?
     * @return The formatted line
     */
    private static String statLine(String key, Object value, boolean newline, boolean showWhen) {
        return showWhen ? (ChatColor.GOLD + key + ": " + ChatColor.AQUA + value.toString() + (newline ? "\n" : "")) : "";
    }

    private static String statLine(String key, Object value) {
        return statLine(key, value, true, true);
    }

    private static String statLine(String key, Object value, boolean showWhen) {
        return statLine(key, value, true, showWhen);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
