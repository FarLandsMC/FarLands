package net.farlands.sanctuary.command.discord;

import com.kicas.rp.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandNotes extends DiscordCommand {
    public CommandNotes() {
        super(Rank.JR_BUILDER, "View, clear, or add notes to a player.", "/notes <view|add|clear> <player> [note]", "notes");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
        if (flp == null) {
            return error(sender, "Player not found.");
        }

        Action action = Utils.valueOfFormattedName(args[0], Action.class);
        if (action == null) {
            return error(sender, "Invalid action: %s", args[0]);
        }

        switch (action) {
            case VIEW:
                if (flp.notes.isEmpty())
                    info(sender, "%s does not have any notes.", flp.username);
                else {
                    if (sender instanceof DiscordSender) {
                        EmbedBuilder eb = new EmbedBuilder()
                            .setTitle("Notes for " + flp.username)
                            .setColor(NamedTextColor.YELLOW.value());
                        flp.notes.forEach(note -> {
                            String[] parts = note.split(":");
                            eb.addField(parts[0], joinArgsBeyond(0, ":", parts), false);
                        });
                        ((DiscordSender) sender).getChannel().sendMessageEmbeds(eb.build()).queue();
                    } else {
                        sender.sendMessage(
                            ComponentColor.gold("Showing notes for ")
                                               .append(ComponentColor.aqua(flp.username))
                                .append(ComponentColor.gold(":\n"))
                                .append(ComponentColor.gray(String.join("\n", flp.notes)))
                        );
                    }
                }
                break;

            case ADD:
                flp.notes.add(FLUtils.dateToString(System.currentTimeMillis(), "MM/dd/yyyy") + " " +
                        sender.getName() + ": " + joinArgsBeyond(1, " ", args));
                info(sender, "Notes added.");
                break;

            case CLEAR:
                flp.notes.clear();
                info(sender, "Cleared notes of %s", flp.username);
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        if (args.length <= 1) {
            return Arrays.stream(Action.VALUES)
                    .map(Utils::formattedName)
                    .filter(action -> action.startsWith(args.length == 0 ? "" : args[0]))
                    .collect(Collectors.toList());
        } else if (args.length == 2)
            return getOnlinePlayers(args[1], sender);
        else
            return Collections.emptyList();
    }

    private enum Action {
        VIEW, ADD, CLEAR;

        static final Action[] VALUES = values();
    }
}
