package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandList extends Command {
    public CommandList() {
        super(Rank.INITIATE, "See the players currently online.", "/list", "list", "who");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        final boolean showVanished = sender instanceof DiscordSender ? ((DiscordSender) sender).getChannel().getIdLong() ==
                FarLands.getFLConfig().getDiscordBotConfig().getChannels().get("staffcommands") : Rank.getRank(sender).isStaff();

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
            sender.sendMessage(ChatColor.RED + "There are no players online currently.");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append("- ").append(total).append(" Player").append(total != 1 ? "s" : "")
                .append(" Online -\n");
        if (!players.isEmpty()) {
            players.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor())
                    .append(rank.getSymbol()).append(": ").append(ChatColor.GOLD)
                    .append(String.join(", ", players.get(rank))).append('\n'));
        }
        if (!staff.isEmpty()) {
            if (!players.isEmpty())
                sb.append(ChatColor.GOLD).append("- Staff -\n");

            staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor())
                    .append(rank.getSymbol()).append(": ").append(ChatColor.GOLD)
                    .append(String.join(", ", staff.get(rank))).append('\n'));
        }

        if (listHasVanishedPlayer)
            sb.append("\n*These players are vanished.");

        sender.sendMessage(sb.toString().trim());

        return true;
    }
}
