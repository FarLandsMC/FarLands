package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandStats extends Command {

    public CommandStats() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Show the stats of a player.", "/stats [playername]", "stats");
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
            if (sender instanceof DiscordSender) {
                Map<PlayerStat, Object> playerInfoMap = playerInfoMap(flp, false, true);
                EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("`" + flp.username + "`'s stats")
                    .setColor(flp.getDisplayRank().getColor().getColor());
//                playerInfoMap.forEach((k, v) -> embedBuilder.addField(k.humanName, v.toString(), false));
                for (PlayerStat stat : PlayerStat.values()) {
                    String value = playerInfoMap.getOrDefault(stat, "").toString();
                    if (!value.isEmpty()) {
                        embedBuilder.addField(stat.humanName, ChatColor.stripColor(value), false);
                    }
                }

                String textureUrl = FLUtils.getSkinUrl(flp);

                if (textureUrl != null) {
                    String headUrl = "https://minecraft-heads.com/scripts/3d-head.php?hrh=00&aa=true&headOnly=true&ratio=6&imageUrl=" +
                        textureUrl.substring(textureUrl.lastIndexOf('/') + 1);
                    embedBuilder.setThumbnail(headUrl);
                }


                ((DiscordSender) sender).getChannel().sendMessage(embedBuilder.build()).queue();
            } else {
                sender.sendMessage(getFormattedStats(flp, isPersonal && sender instanceof Player));
            }
        });
        return true;
    }

    /**
     * Get player info into a map
     * <br>
     * Current items:
     * <pre>
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
    public static Map<PlayerStat, Object> playerInfoMap(OfflineFLPlayer flp, boolean showDonated, boolean showTimezone) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(flp.uuid);

        Map<PlayerStat, Object> statsMap = new HashMap<>();

        if (flp.nickname != null && !flp.nickname.isEmpty()) {
            statsMap.put(PlayerStat.NICKNAME, flp.nickname);
        }
        if (flp.pronouns != null && flp.pronouns.toString() != null) {
            statsMap.put(PlayerStat.PRONOUNS, flp.pronouns.toString(false));
        }
        statsMap.put(PlayerStat.RANK, flp.rank.getColor() + (flp.rank.isStaff() ? ChatColor.BOLD + "" : "") + flp.rank.getName());
        statsMap.put(PlayerStat.TIME_PLAYED, TimeInterval.formatTime(flp.secondsPlayed * 1000L, false));
        if (showDonated && flp.amountDonated > 0) {
            statsMap.put(PlayerStat.AMOUNT_DONATED, "$" + flp.amountDonated);
        }
        statsMap.put(PlayerStat.DEATHS, flp.deaths);
        if (flp.birthday != null) {
            statsMap.put(PlayerStat.BIRTHDAY, flp.birthday.toFormattedString());
        }
        // TODO: Fix Chat#atPlayer and remove showTimezone param
        if (showTimezone && flp.timezone != null && !flp.timezone.isEmpty()) {
            statsMap.put(PlayerStat.TIMEZONE, flp.timezone + " (" + ChatColor.GREEN + flp.currentTime() +  ChatColor.AQUA + ")");
        }
        statsMap.put(PlayerStat.VOTES_THIS_MONTH, flp.monthVotes);
        statsMap.put(PlayerStat.TOTAL_SEASON_VOTES, flp.totalSeasonVotes);
        statsMap.put(PlayerStat.TOTAL_VOTES, flp.totalVotes);

        return statsMap;
    }

    public static String formatStats(Map<PlayerStat, Object> statsMap, OfflineFLPlayer flp) {
        Rank displayedRank = flp.getDisplayRank();

        List<String> out = new ArrayList<>();
        out.add( // Add the header
                (displayedRank.compareTo(Rank.SCHOLAR) > 0 ? displayedRank.getColor() : "")
                + flp.username + ChatColor.GOLD + "'s Stats: " + ChatColor.GOLD
        );
        for (PlayerStat stat : PlayerStat.values()) {
            String value = statsMap.getOrDefault(stat, "").toString();
            if (!value.isEmpty()) {
                out.add(ChatColor.GOLD + stat.humanName + ": " + ChatColor.AQUA + value);
            }
        }

        return String.join("\n", out);
    }

    public static String getFormattedStats(OfflineFLPlayer flp, boolean showDonated) {
        return formatStats(playerInfoMap(flp, showDonated, true), flp);
    }

    public static String getFormattedStats(OfflineFLPlayer flp, boolean showDonated, boolean showTimezone) {
        return formatStats(playerInfoMap(flp, showDonated, showTimezone), flp);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }

    public enum PlayerStat {
        NICKNAME("Nickname"),
        PRONOUNS("Pronouns"),
        RANK("Rank"),
        TIME_PLAYED("Time Played"),
        AMOUNT_DONATED("Amount Donated"),
        BIRTHDAY("Birthday"),
        TIMEZONE("Time Zone"),
        DEATHS("Deaths"),
        VOTES_THIS_MONTH("Votes This Month"),
        TOTAL_SEASON_VOTES("Total Votes This Season"),
        TOTAL_VOTES("Total Votes All Time");

        public static final PlayerStat[] values = values();

        public final String humanName;

        PlayerStat(String humanName) {
            this.humanName = humanName;
        }
    }
}
