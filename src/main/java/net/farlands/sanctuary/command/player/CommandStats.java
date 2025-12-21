package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandStats extends Command {

    public CommandStats() {
        super(
            CommandData.simple(
                "stats",
                "Show the stats of a player.",
                "/stats [playername]"
            )
            .aliases("vp")
            .category(Category.PLAYER_SETTINGS_AND_INFO)
         );
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
            if (sender instanceof DiscordSender ds) {
                Map<PlayerStat, Object> playerInfoMap = playerInfoMap(flp, sender, false, true);
                EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Stats for `" + flp.username + "`")
                    .setColor(flp.getDisplayRank().color().value());
                for (PlayerStat stat : PlayerStat.values()) {
                    Object value = playerInfoMap.getOrDefault(stat, "");
                    String str = (value instanceof ComponentLike c) ? ComponentUtils.toText(c.asComponent()) : value.toString();
                    str = MarkdownProcessor.removeChatColor(str);
                    if (!str.isEmpty()) {
                        embedBuilder.addField(stat.humanName, str, false);
                    }
                }

                String headUrl = FLUtils.getHeadUrl(flp);

                if (headUrl != null) {
                    embedBuilder.setThumbnail(headUrl);
                }


                ds.sendMessageEmbeds(embedBuilder.build());
            } else {
                sender.sendMessage(getFormattedStats(flp, sender, isPersonal && sender instanceof Player));
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
     * @param requester   the command sender to use for locale purposes
     * @param showDonated Whether to show amount of money donated
     * @return the properly formatted text
     */
    public static Map<PlayerStat, Object> playerInfoMap(OfflineFLPlayer flp, @Nullable CommandSender requester, boolean showDonated, boolean showTimezone) {
        Map<PlayerStat, Object> statsMap = new HashMap<>();

        if (flp.nickname != null) {
            statsMap.put(PlayerStat.NICKNAME, flp.nickname);
        }
        if (flp.pronouns != null && flp.pronouns.toString() != null) {
            statsMap.put(PlayerStat.PRONOUNS, flp.pronouns.toString(false));
        }
        statsMap.put(PlayerStat.RANK, flp.rank);
        statsMap.put(PlayerStat.TIME_PLAYED, flp.secondsPlayed < 1000L ? "Not played this season." : TimeInterval.formatTime(flp.secondsPlayed * 1000L, false));
        if (showDonated && flp.amountDonated > 0) {
            statsMap.put(PlayerStat.AMOUNT_DONATED, "$" + flp.amountDonated);
        }
        statsMap.put(PlayerStat.DEATHS, flp.deaths);
        if (flp.birthday != null) {
            statsMap.put(PlayerStat.BIRTHDAY, flp.birthday.toFormattedString());
        }
        if (showTimezone && flp.timezone != null && !flp.timezone.isEmpty()) {
            statsMap.put(PlayerStat.TIMEZONE, ComponentColor.aqua("{} ({:green})", flp.timezone, flp.currentTime(requester)));
        }
        statsMap.put(PlayerStat.VOTES_MONTH, flp.monthVotes);
        statsMap.put(PlayerStat.VOTES_SEASON, flp.totalSeasonVotes);
        statsMap.put(PlayerStat.VOTES_ALL, flp.totalVotes);

        return statsMap;
    }

    public static Component formatStats(Map<PlayerStat, Object> statsMap, OfflineFLPlayer flp) {
        TextComponent.Builder builder = Component.text()
            .color(NamedTextColor.GOLD)
            .append(ComponentColor.gold("{}'s Stats: ", flp.rank.colorName(flp.username)));

        for (PlayerStat stat : PlayerStat.values()) {
            Object value = statsMap.getOrDefault(stat, "");
            if (value != null && !value.toString().isEmpty()) {
                builder.append(ComponentColor.gold(
                    "\n{}: {}",
                    stat.humanName,
                    value instanceof ComponentLike c ? c : ComponentColor.aqua(value.toString())
                ));
            }
        }
        return builder.build();
    }

    public static Component getFormattedStats(OfflineFLPlayer flp, @Nullable CommandSender requester,  boolean showDonated) {
        return formatStats(playerInfoMap(flp, requester, showDonated, true), flp);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false)
            .addOption(OptionType.STRING, "playername", "The player to query", false, true);
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
        VOTES_MONTH("Votes This Month"),
        VOTES_SEASON("Total Votes This Season"),
        VOTES_ALL("Total Votes All Time");

        public static final PlayerStat[] values = values();

        public final String humanName;

        PlayerStat(String humanName) {
            this.humanName = humanName;
        }
    }
}
