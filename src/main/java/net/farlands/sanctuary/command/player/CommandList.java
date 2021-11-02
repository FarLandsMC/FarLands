package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
            staffTotal = 0,
            overworld = 0,
            farlands = 0,
            nether = 0,
            end = 0;
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

            switch (player.getWorld().getName()) {
                case "farlands": {
                    ++farlands;
                    break;
                }
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
            if (farlands > 0)
                embedDesc.add("Pocket: " + farlands);
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
                    eb.addField(
                        rank.getName(),
                        Chat.applyDiscordFilters(String.join(", ", players.get(rank))),
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
                            Chat.applyDiscordFilters(String.join(", ", staff.get(rank))),
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
                    cb.append(
                            Component.text(rank.getName() + ": ")
                                .color(TextColor.color(rank.getColor().getColor().getRGB()))
                        )
                        .append(
                            ComponentColor.gold(String.join(", ", players.get(rank)) + '\n')
                        );
                });
            }
            if (!staff.isEmpty()) {
                if (!players.isEmpty())
                    cb.append(ComponentColor.gold("- Staff -\n"));

                staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> {
                    cb.append(
                        Component.text(rank.getName() + ": ")
                            .color(TextColor.color(rank.getColor().getColor().getRGB()))
                    ).append(
                        ComponentColor.gold(String.join(", ", staff.get(rank)) + "\n")
                    );
                });
            }

            boolean applyColour = overworld > 0 || nether > 0 || end > 0 || farlands > 0;

            StringBuilder worlds = new StringBuilder();

            if (overworld > 0)
                worlds.append("\nOverworld: ").append(overworld);
            if (farlands > 0)
                worlds.append("\nPocket: ").append(farlands);
            if (nether > 0)
                worlds.append("\nNether: ").append(nether);
            if (end > 0)
                worlds.append("\nEnd: ").append(end);

            if (!worlds.isEmpty()) {
                cb.append(ComponentColor.aqua(worlds.toString()));
            }

            if (listHasVanishedPlayer)
                cb.append(ComponentColor.gold("\n*These players are vanished."));

            sender.sendMessage(cb.build());
        }

        return true;
    }
}
