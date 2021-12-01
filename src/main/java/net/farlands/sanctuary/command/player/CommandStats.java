package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            sender.sendMessage(ComponentColor.red("Player not found."));
            return true;
        }
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            flp.updateAll(false); // Make sure our stats are fresh
            if (sender instanceof DiscordSender) {
                Map<PlayerStat, Object> playerInfoMap = playerInfoMap(flp, false, true);
                EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("`" + flp.username + "`'s stats")
                    .setColor(flp.getDisplayRank().getColor().getColor());
                for (PlayerStat stat : PlayerStat.values()) {
                    Object value = playerInfoMap.getOrDefault(stat, "");
                    String str = (value instanceof Component c) ? ComponentUtils.toText(c) : value.toString();
                    if (!str.isEmpty()) {
                        embedBuilder.addField(stat.humanName, str, false);
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
        Map<PlayerStat, Object> statsMap = new HashMap<>();

        if (flp.nickname != null && !flp.nickname.isEmpty()) {
            statsMap.put(PlayerStat.NICKNAME, flp.nickname);
        }
        if (flp.pronouns != null && flp.pronouns.toString() != null) {
            statsMap.put(PlayerStat.PRONOUNS, flp.pronouns.toString(false));
        }
        statsMap.put(PlayerStat.RANK, flp.rank.getLabel());
        statsMap.put(PlayerStat.TIME_PLAYED, TimeInterval.formatTime(flp.secondsPlayed * 1000L, false));
        if (showDonated && flp.amountDonated > 0) {
            statsMap.put(PlayerStat.AMOUNT_DONATED, "$" + flp.amountDonated);
        }
        statsMap.put(PlayerStat.DEATHS, flp.deaths);
        if (flp.birthday != null) {
            statsMap.put(PlayerStat.BIRTHDAY, flp.birthday.toFormattedString());
        }
        if (showTimezone && flp.timezone != null && !flp.timezone.isEmpty()) {
            statsMap.put(PlayerStat.TIMEZONE, ComponentColor.aqua(flp.timezone + "(")
                .append(ComponentColor.green(flp.currentTime()))
                .append(ComponentColor.aqua(")"))
            );
        }
        statsMap.put(PlayerStat.VOTES_THIS_MONTH, flp.monthVotes);
        statsMap.put(PlayerStat.TOTAL_SEASON_VOTES, flp.totalSeasonVotes);
        statsMap.put(PlayerStat.TOTAL_VOTES, flp.totalVotes);

        return statsMap;
    }

    public static Component formatStats(Map<PlayerStat, Object> statsMap, OfflineFLPlayer flp) {
        TextComponent.Builder builder = Component.text()
            .color(NamedTextColor.GOLD)
            .append(
                flp.rank.colorName(flp.username)
                    .append(ComponentColor.gold("'s Stats: "))
            );

        for (PlayerStat stat : PlayerStat.values()) {
            Object value = statsMap.getOrDefault(stat, "");
            if (value != null) {
                builder.append(Component.newline())
                    .append(ComponentColor.gold(stat.humanName))
                    .append(ComponentColor.gold(": "))
                    .append((value instanceof Component c) ? c : ComponentColor.aqua(value.toString()));
            }
        }
        return builder.build();
    }

    public static Component getFormattedStats(OfflineFLPlayer flp, boolean showDonated) {
        return formatStats(playerInfoMap(flp, showDonated, true), flp);
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
