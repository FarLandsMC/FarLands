package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;

import net.farlands.sanctuary.discord.DiscordChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandList extends Command {
    public CommandList() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "See the players currently online.", "/list", "list", "who");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        final boolean showVanished = sender instanceof DiscordSender
                ? ((DiscordSender) sender).getChannel().getIdLong() ==
                        FarLands.getFLConfig().discordBotConfig.channels.get(DiscordChannel.STAFF_COMMANDS)
                : Rank.getRank(sender).isStaff();

        Map<Rank, List<String>> players = new HashMap<>(), staff = new HashMap<>(), bucket;
        int total = 0, overworld = 0, nether = 0, end = 0;
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

            switch (player.getWorld().getName()) {
                case "farlands":
                case "world": {
                    ++overworld;
                    break;
                }
                case "world_nether": {
                    ++nether;
                    break;
                }
                case "world_the_end": {
                    ++end;
                    break;
                }
            }

            bucket.get(flp.rank).add(name);
            ++total;
        }

        if (players.size() + staff.size() == 0) {
            sendFormatted(sender, "&(gold)There are no players online currently.");
            return true;
        }

        if (sender instanceof DiscordSender) {
            List<String> embedDesc = new ArrayList<>();
            if (overworld > 0)
                embedDesc.add("Overworld: " + overworld);
            if (nether > 0)
                embedDesc.add("Nether: " + nether);
            if (end > 0)
                embedDesc.add("End: " + end);

            EmbedBuilder eb = new EmbedBuilder()
                .setTitle(+ total + " Player" + (total == 1 ? "" : "s") + " Online")
                .setDescription(String.join(" | ", embedDesc) +
                        (listHasVanishedPlayer ? "\n*\\*These players are vanished*" : ""))
                .setColor(0x00AAAA); // DARK_AQUA

            if (!players.isEmpty()) {
                players.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank ->
                    eb.addField(rank.getName(), String.join(", ", players.get(rank)), false)
                );
            }

            if (!staff.isEmpty()) {
                if (!players.isEmpty())
                    eb.addField("— Staff —", staff.keySet().size() + " Staff Member" +
                            (staff.keySet().size() == 1 ? "" : "s")
                            + " Online", false);

                staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank ->
                        eb.addField(rank.getName(), String.join(", ", staff.get(rank)), false)
                );
            }

            ((DiscordSender) sender).getChannel().sendMessage(eb.build()).queue();

        } else {

            StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.GOLD).append("- ").append(total).append(" Player").append(total != 1 ? "s" : "")
                    .append(" Online -\n");
            if (!players.isEmpty()) {
                players.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor())
                        .append(rank.getName()).append(": ").append(ChatColor.GOLD)
                        .append(String.join(", ", players.get(rank))).append('\n'));
            }
            if (!staff.isEmpty()) {
                if (!players.isEmpty())
                    sb.append(ChatColor.GOLD).append("- Staff -\n");

                staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor())
                        .append(rank.getName()).append(": ").append(ChatColor.GOLD)
                        .append(String.join(", ", staff.get(rank))).append('\n'));
            }

            boolean applyColour = overworld > 0 || nether > 0 || end > 0;
            if (applyColour)
                sb.append(ChatColor.AQUA);

            if (overworld > 0)
                sb.append("\nOverworld: ").append(overworld);
            if (nether > 0)
                sb.append("\nNether: ").append(nether);
            if (end > 0)
                sb.append("\nEnd: ").append(end);

            if (applyColour)
                sb.append(ChatColor.GOLD);
            if (listHasVanishedPlayer)
                sb.append("\n*These players are vanished.");

            sender.sendMessage(sb.toString().trim());
        }

        return true;
    }
}
