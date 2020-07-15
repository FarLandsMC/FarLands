package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.*;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandTop extends Command {
    public CommandTop() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the people with the most votes or play time.",
                "/top <votes|playtime|donors|deaths> [page|no-staff|month|all]", "top");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        List<OfflineFLPlayer> flps;
        TopCategory category = Utils.valueOfFormattedName(args[0], TopCategory.class);
        if (category == null)
            return false;

        int pageMax;

        switch (category) {
            case VOTES: {
                flps = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                        .filter(flp -> flp.totalVotes > 0)
                        .collect(Collectors.toList());
                pageMax = flps.size() / 10 + 1;
                int offset = getOffset(sender, flps.size(), args);
                if (offset == -1)
                    return true;

                OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

                // Month votes
                if (args.length == 1 || !"all".equals(args[1])) {
                    flps.sort(Collections.reverseOrder(Comparator.comparingInt(flp -> flp.monthVotes * 65536 + flp.totalSeasonVotes)));
                    int position = position(senderFlp, flps, flp -> flp.uuid);

                    sendFormatted(sender, "&(gold)Showing the top voters for this month (page %0/%1):", offset / 10 + 1, pageMax);
                    for (int i = offset; i < Math.min(flps.size(), offset + 10); ++i) {
                        sendFormatted(
                                sender,
                                "&(gold){&(%0)%1:} {&(aqua)%2} - %3 $(inflect,noun,3,vote) this month, %4 total $(inflect,noun,4,vote)",
                                i == position ? "green" : "gold",
                                i + 1,
                                flps.get(i).username,
                                flps.get(i).monthVotes,
                                flps.get(i).totalSeasonVotes
                        );
                    }
                    if (position != -1) {
                        sendFormatted(
                                sender,
                                "&(gold)You are {&(aqua)#%0} - %1 $(inflect,noun,1,vote) this month, %2 total $(inflect,noun,2,vote)",
                                position + 1,
                                senderFlp.monthVotes,
                                senderFlp.totalSeasonVotes
                        );
                    }
                }
                // All votes
                else {
                    flps.sort(Collections.reverseOrder(Comparator.comparingInt(flp -> flp.totalVotes)));
                    int position = position(senderFlp, flps, flp -> flp.uuid);

                    sendFormatted(sender, "&(gold)Showing the top voters of all time (page %0/%1):", offset / 10 + 1, pageMax);
                    for (int i = offset; i < Math.min(flps.size(), offset + 10); ++i) {
                        sendFormatted(
                                sender,
                                "&(gold){&(%0)%1:} {&(aqua)%2} - %3 $(inflect,noun,3,vote)",
                                i == position ? "green" : "gold",
                                i + 1,
                                flps.get(i).username,
                                flps.get(i).totalVotes
                        );
                    }
                    if (position != -1) {
                        sendFormatted(
                                sender,
                                "&(gold)You are {&(green)#%0} - %1 $(inflect,noun,1,vote)",
                                position + 1,
                                senderFlp.totalVotes
                        );
                    }
                }
                break;
            }

            case PLAYTIME: {
                flps = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                        .filter(flp -> (args.length <= 1 || !"no-staff".equals(args[1]) || !flp.rank.isStaff()) && flp.secondsPlayed > 0)
                        .sorted(Collections.reverseOrder(Comparator.comparingInt(flp -> flp.secondsPlayed)))
                        .collect(Collectors.toList());

                pageMax = flps.size() / 10 + 1;
                int offset = getOffset(sender, pageMax, args);
                if (offset == -1)
                    return true;

                OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
                int position = position(senderFlp, flps, flp -> flp.uuid);

                sendFormatted(sender, "&(gold)Showing the top players with the longest play time (page %0/%1):", offset / 10 + 1, pageMax);
                for (int i = offset; i < Math.min(flps.size(), offset + 10); ++i) {
                    sendFormatted(
                            sender,
                            "&(gold){&(%0)%1:} {&(aqua)%2} - %3",
                            i == position ? "green" : "gold",
                            i + 1,
                            flps.get(i).username,
                            TimeInterval.formatTime(1000L * flps.get(i).secondsPlayed, true)
                    );
                }
                if (position != -1) {
                    sendFormatted(
                            sender,
                            "&(gold)You are {&(green)#%0} - %1",
                            position + 1,
                            TimeInterval.formatTime(1000L * senderFlp.secondsPlayed, true)
                    );
                }
                break;
            }

            case DONORS: {
                flps = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                        .filter(flp -> flp.amountDonated > 0)
                        .sorted(Collections.reverseOrder(Comparator.comparingDouble(flp -> flp.amountDonated)))
                        .collect(Collectors.toList());

                pageMax = flps.size() / 10 + 1;
                int offset = getOffset(sender, pageMax, args);
                if (offset == -1)
                    return true;

                OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
                int position = position(senderFlp, flps, flp -> flp.uuid);

                sendFormatted(sender, "&(gold)Showing the top server donors (page %0/%1):", offset / 10 + 1, pageMax);
                for (int i = offset; i < Math.min(flps.size(), offset + 10); ++i) {
                    sendFormatted(
                            sender,
                            "&(gold){&(%0)%1:} &(aqua)%2",
                            i == position ? "green" : "gold",
                            i + 1,
                            flps.get(i).username
                    );
                }
                if (position != -1)
                    sendFormatted(sender, "&(gold)You are {&(green)#%0}", position + 1);
                break;
            }

            case DEATHS: {
                List<OfflinePlayer> topDeaths = Arrays.stream(Bukkit.getOfflinePlayers())
                        .filter(player -> player.getStatistic(Statistic.DEATHS) > 0)
                        .sorted(Collections.reverseOrder(Comparator.comparingInt(player -> player.getStatistic(Statistic.DEATHS))))
                        .collect(Collectors.toList());

                pageMax = topDeaths.size() / 10 + 1;
                int offset = getOffset(sender, pageMax, args);
                if (offset == -1)
                    return true;

                OfflinePlayer senderOfflinePlayer = Bukkit.getOfflinePlayer(FarLands.getDataHandler().getOfflineFLPlayer(sender).uuid);
                int position = position(senderOfflinePlayer, topDeaths, OfflinePlayer::getUniqueId);

                sendFormatted(sender, "&(gold)Showing the players with the most deaths (page %0/%1):", offset / 10 + 1, pageMax);
                for (int i = offset; i < Math.min(topDeaths.size(), offset + 10); ++i) {
                    sendFormatted(
                            sender,
                            "&(gold){&(%0)%1:} &(aqua)%2 - %3 $(inflect,noun,3,death)",
                            i == position ? "green" : "gold",
                            i + 1,
                            topDeaths.get(i).getName(),
                            topDeaths.get(i).getStatistic(Statistic.DEATHS)
                    );
                }
                if (position != -1) {
                    sendFormatted(
                            sender,
                            "&(gold)You are {&(green)#%0} - %1 $(inflect,noun,1,death)",
                            position + 1,
                            senderOfflinePlayer.getStatistic(Statistic.DEATHS)
                    );
                }
                break;
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length == 1) {
            return Arrays.stream(TopCategory.VALUES)
                    .map(Utils::formattedName)
                    .filter(o -> o.startsWith(args.length == 0 ? "" : args[0]))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            TopCategory category = Utils.valueOfFormattedName(args[0], TopCategory.class);
            if (category == null)
                return Collections.emptyList();

            if (category == TopCategory.VOTES) {
                return Stream.of("month", "all")
                        .filter(o -> o.startsWith(args[1]))
                        .collect(Collectors.toList());
            } else if (category == TopCategory.PLAYTIME) {
                return Collections.singletonList("no-staff");
            }
        }

        return Collections.emptyList();
    }

    private static int getOffset(CommandSender sender, int max, String[] args) {
        int offset = 1;
        if (args.length > 1) {
            String page = args[args.length == 2 ? 1 : 2];

            try {
                offset = Integer.parseInt(page);
            } catch (NumberFormatException ex) { }

            if (offset <= 0 || offset > max) {
                sendFormatted(sender, "&(red)The page number must be between %0 and %1", 1, max);
                return -1;
            }
        }
        return (offset - 1) * 10;
    }

    private static <T> int position(T object, Collection<T> collection, Function<T, UUID> toUuid) {
        int pos = 0;
        UUID uuid = toUuid.apply(object);
        for (T other : collection) {
            if (uuid.equals(toUuid.apply(other)))
                return pos;
            pos += 1;
        }

        return -1;
    }

    enum TopCategory {
        VOTES, PLAYTIME, DONORS, DEATHS;

        static final TopCategory[] VALUES = values();
    }
}
