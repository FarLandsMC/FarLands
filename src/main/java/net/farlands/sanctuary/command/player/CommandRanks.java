package net.farlands.sanctuary.command.player;

import com.kicas.rp.util.ReflectionHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandHandler;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandRanks extends Command {
    public CommandRanks() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "Show all available player ranks.", "/ranks", "ranks");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(CommandSender sender, String[] args) {
        Map<Rank, List<String>> ranksInfo = new HashMap<>();

        for (Rank rank : Rank.VALUES) {
            // Skip the voter and birthday ranks as it's only used internally
            if (Rank.VOTER == rank || Rank.BIRTHDAY == rank)
                continue;

            // Don't show staff ranks
            if (rank.specialCompareTo(Rank.SPONSOR) > 0)
                break;

            List<String> info = new ArrayList<>();

            // For donor ranks, show the cost
            switch (rank) {
                case DONOR:
                case PATRON:
                case SPONSOR:
                    info.add(Rank.DONOR_RANK_COSTS[rank.ordinal() - Rank.DONOR.ordinal()] + " USD");
                    break;

                default: {
                    int playTimeRequired = rank.getPlayTimeRequired();
                    // Ignore the initiate rank
                    if (playTimeRequired > 0)
                        info.add(TimeInterval.formatTime(playTimeRequired * 60L * 60L * 1000L, false) + " play-time");
                }
            }

            // Add homes
            info.add(rank.getHomes() + (rank.getHomes() == 1 ? " home" : " homes"));

            // Specify the new commands that come with the rank
            if (!Rank.INITIATE.equals(rank)) {
                List<String> cmds = ((List<Command>) ReflectionHelper.getFieldValue("commands", CommandHandler.class, FarLands.getCommandHandler()))
                        .stream().filter(cmd -> rank.equals(cmd.getMinRankRequirement()))
                        .map(cmd -> "/" + cmd.getName().toLowerCase())
                        .collect(Collectors.toList());

                if (!cmds.isEmpty())
                    info.add(String.join(", ", cmds));
            }

            ranksInfo.put(rank, info);
        }

        if (sender instanceof DiscordSender ds) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("Ranks")
                    .setColor(flp == null ? 0xAAAAAA : flp.rank.color().value());
            for (Rank rank : Rank.values()) {
                if (!ranksInfo.containsKey(rank)) { continue; }
                eb.addField(
                    rank.getName(),
                    String.join("\n- ", ranksInfo.get(rank)),
                    false
                );
            };
            ds.sendMessageEmbeds(eb.build());

        } else {
            var b = ((TextComponent) ComponentColor.green("Ranks: \n")).toBuilder();
            for (Rank rank : Rank.values()) {
                if (!ranksInfo.containsKey(rank)) { continue; }
                b.append(ComponentColor.blue("{} - {}\n", rank, String.join(" - ", ranksInfo.get(rank))));
            }
            sender.sendMessage(b);
        }

        return true;
    }
}
