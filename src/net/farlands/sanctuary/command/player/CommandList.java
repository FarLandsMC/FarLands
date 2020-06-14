package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;

import net.farlands.sanctuary.discord.DiscordChannel;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandList extends Command {
    public CommandList() {
        super(Rank.INITIATE, "See the players currently online.", "/list", "list", "who");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        final boolean showVanished = sender instanceof DiscordSender
                ? ((DiscordSender) sender).getChannel().getIdLong() ==
                        FarLands.getFLConfig().discordBotConfig.channels.get(DiscordChannel.STAFF_COMMANDS)
                : Rank.getRank(sender).isStaff();

        Map<Rank, List<String>> players = new HashMap<>(), staff = new HashMap<>(), bucket;
        int total = 0;
        boolean listHasVanishedPlayer = false;
        OfflineFLPlayer flp;
        for (Player player : Bukkit.getOnlinePlayers()) {
            flp = FarLands.getDataHandler().getOfflineFLPlayer(player);

            if (!flp.rank.isStaff() && !flp.vanished)
                bucket = players;
            else if (flp.rank.isStaff() && (!flp.vanished || showVanished))
                bucket = staff;
            else
                continue;

            String name = flp.username;
            if (flp.vanished) {
                name += "*";
                listHasVanishedPlayer = true;
            }

            if (!bucket.containsKey(flp.rank))
                bucket.put(flp.rank, new ArrayList<>());

            bucket.get(flp.rank).add(name);
            ++total;
        }

        if (players.size() + staff.size() == 0) {
            sendFormatted(sender, "&(gold)There are no players online currently.");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("&(gold)- ").append(total).append(" Player").append(total != 1 ? "s" : "").append(" Online -\n");
        if (!players.isEmpty()) {
            players.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor())
                    .append(rank.getName()).append(": &(gold)").append(String.join(", ", players.get(rank))).append('\n'));
        }
        if (!staff.isEmpty()) {
            if (!players.isEmpty())
                sb.append("&(gold)- Staff -\n");

            staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor())
                    .append(rank.getName()).append(": &(gold)").append(String.join(", ", staff.get(rank))).append('\n'));
        }

        if (listHasVanishedPlayer)
            sb.append("\n*These players are vanished.");

        sendFormatted(sender, sb.toString().trim());

        return true;
    }
}
