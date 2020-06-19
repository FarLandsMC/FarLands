package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandTop extends Command {
    public CommandTop() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the people with the most votes or play time.",
                "/top <votes|playtime|donors> [month|all]", "top");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;
        List<OfflineFLPlayer> flps = FarLands.getDataHandler().getOfflineFLPlayers();
        if ("votes".equals(args[0])) {
            if (args.length == 1 || "month".equals(args[1])) {
                flps.sort(Collections.reverseOrder(Comparator.comparingInt(flp -> flp.monthVotes * 65536 + flp.totalSeasonVotes)));
                sendFormatted(sender, "&(gold)Showing the top voters for this month:");
                for (int i = 0; i < Math.min(flps.size(), 10); ++i) {
                    sendFormatted(sender, "&(gold)%0: {&(aqua)%1} - %2 $(inflect,noun,2,vote) this month, %3 total " +
                                    "$(inflect,noun,3,vote)", i + 1, flps.get(i).username, flps.get(i).monthVotes,
                            flps.get(i).totalSeasonVotes);
                }
            } else if ("all".equals(args[1])) {
                flps.sort(Collections.reverseOrder(Comparator.comparingInt(flp -> flp.totalVotes)));
                sendFormatted(sender, "&(gold)Showing the top voters of all time:");
                for (int i = 0; i < Math.min(flps.size(), 10); ++i) {
                    sendFormatted(sender, "&(gold)%0: {&(aqua)%1} - %2 $(inflect,noun,2,vote)", i + 1, flps.get(i).username,
                            flps.get(i).totalVotes);
                }
            } else
                return false;
        } else if ("playtime".equals(args[0])) {
            flps.sort(Collections.reverseOrder(Comparator.comparingInt(flp -> flp.secondsPlayed)));
            sendFormatted(sender, "&(gold)Showing the top players with the longest play time:");
            for (int i = 0; i < Math.min(flps.size(), 10); ++i) {
                sendFormatted(sender, "&(gold)%0: {&(aqua)%1} - %2", i + 1, flps.get(i).username,
                        TimeInterval.formatTime(1000L * flps.get(i).secondsPlayed, true));
            }
        } else if ("donors".equals(args[0])) {
            flps.sort(Collections.reverseOrder(Comparator.comparingInt(flp -> flp.amountDonated)));
            sendFormatted(sender, "&(gold)Showing the top server donors:");
            for (int i = 0; i < Math.min(flps.size(), 10); ++i)
                sendFormatted(sender, "&(gold)%0: &(aqua)%1", i + 1, flps.get(i).username);
        } else
            return false;
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? Stream.of("votes", "playtime", "donors").filter(o -> o.startsWith(args.length == 0 ? "" : args[0])).collect(Collectors.toList()) :
                ("votes".equals(args[0]) ? Stream.of("month", "all").filter(o -> o.startsWith(args[1])).collect(Collectors.toList()) : Collections.emptyList());
    }
}
