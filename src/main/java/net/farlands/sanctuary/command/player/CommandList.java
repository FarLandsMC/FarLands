package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kicas.rp.util.TextUtils.sendFormatted;

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



        Map<Rank, List<String>>
            players = new HashMap<>(),
            staff = new HashMap<>(),
            bucket;

        int total = 0,
            staffTotal = 0;

        Map<Worlds, Integer> worlds = new HashMap<>();

        boolean listHasVanishedPlayer = false;
        OfflineFLPlayer flp;
        for (Player player : Bukkit.getOnlinePlayers()) {
            flp = FarLands.getDataHandler().getOfflineFLPlayer(player);

            if (!flp.rank.isStaff() && !flp.vanished) {
                bucket = players;
            } else if (flp.rank.isStaff() && (!flp.vanished || showVanished)) {
                bucket = staff;
                ++staffTotal;
            } else {
                continue;
            }

            String name = flp.username;
            if (flp.vanished) {
                name += "*";
                listHasVanishedPlayer = true;
            }

            if (!bucket.containsKey(flp.rank))
                bucket.put(flp.rank, new ArrayList<>());

            // Increment count
            worlds.compute(Worlds.getByWorld(player.getWorld()), (k, v) -> v == null ? 1 : v + 1);

            bucket.get(flp.rank).add(name);
            ++total;
        }

        if (players.size() + staff.size() == 0) {
            sendFormatted(sender, "&(gold)There are no players online currently.");
            return true;
        }

        if (sender instanceof DiscordSender) {
            List<String> embedDesc = new ArrayList<>();

            if (worlds.containsKey(Worlds.OVERWORLD))
                embedDesc.add("Overworld: " + worlds.get(Worlds.OVERWORLD));
            if (worlds.containsKey(Worlds.POCKET))
                embedDesc.add("Pocket: " + worlds.get(Worlds.POCKET));
            if (worlds.containsKey(Worlds.NETHER))
                embedDesc.add("Nether: " + worlds.get(Worlds.NETHER));
            if (worlds.containsKey(Worlds.END))
                embedDesc.add("End: " + worlds.get(Worlds.END));

            EmbedBuilder eb = new EmbedBuilder()
                .setTitle(total + " Player" + (total == 1 ? "" : "s") + " Online")
                .setDescription(String.join(" | ", embedDesc) +
                        (listHasVanishedPlayer ? "\n*\\*These players are vanished*" : ""))
                .setColor(NamedTextColor.DARK_AQUA.value());

            if (!players.isEmpty()) {
                players.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank ->
                    eb.addField(
                        rank.getName(),
                        MarkdownProcessor.escapeMarkdown(String.join(", ", players.get(rank))),
                        false
                    )
                );
            }

            if (!staff.isEmpty()) {
                if (!players.isEmpty())
                    eb.addField("— Staff —", staffTotal + " Staff Member" +
                            (staff.keySet().size() == 1 ? "" : "s")
                            + " Online", false);

                staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank ->
                        eb.addField(
                            rank.getName(),
                            MarkdownProcessor.escapeMarkdown(String.join(", ", staff.get(rank))),
                            false
                        )
                );
            }

            ((DiscordSender) sender).getChannel().sendMessage(eb.build()).queue();

        } else {

            TextComponent.Builder cb = Component.text()
                .content("- " + total + " Player" + (total != 1 ? "s" : "") + " Online -\n")
                .color(NamedTextColor.GOLD);
            if (!players.isEmpty()) {
                players.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> {
                    cb.append(Component.text(rank.getName() + ": ").color(rank.color()))
                        .append(
                            ComponentColor.gold(String.join(", ", players.get(rank)) + '\n')
                        );
                });
            }
            if (!staff.isEmpty()) {
                if (!players.isEmpty())
                    cb.append(ComponentColor.gold("- Staff -\n"));

                staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> {
                    cb.append(Component.text(rank.getName() + ": ").color(rank.color()))
                        .append(
                            ComponentColor.gold(String.join(", ", staff.get(rank)) + "\n")
                        );
                });
            }

            StringBuilder worldSB = new StringBuilder();

            if (worlds.containsKey(Worlds.OVERWORLD))
                worldSB.append("\nOverworld: ").append(worlds.get(Worlds.OVERWORLD));
            if (worlds.containsKey(Worlds.POCKET))
                worldSB.append("\nPocket: ").append(worlds.get(Worlds.POCKET));
            if (worlds.containsKey(Worlds.NETHER))
                worldSB.append("\nNether: ").append(worlds.get(Worlds.NETHER));
            if (worlds.containsKey(Worlds.END))
                worldSB.append("\nEnd: ").append(worlds.get(Worlds.END));

            if (!worldSB.isEmpty()) {
                cb.append(ComponentColor.aqua(worldSB.toString()));
            }

            if (listHasVanishedPlayer)
                cb.append(ComponentColor.gold("\n*These players are vanished."));

            sender.sendMessage(cb.build());
        }

        return true;
    }
}
