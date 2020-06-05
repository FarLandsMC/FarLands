package net.farlands.odyssey.command.discord;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.util.Utils;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.util.FLUtils;
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
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        Action action = Utils.valueOfFormattedName(args[0], Action.class);
        if (action == null) {
            sendFormatted(sender, "&(red)Invalid action: %0", args[0]);
            return true;
        }

        switch (action) {
            case VIEW:
                if (flp.notes.isEmpty())
                    sendFormatted(sender, "&(gold){&(aqua)%0} does not have any notes.", flp.username);
                else {
                    sendFormatted(sender, "&(gold)Showing notes for {&(aqua)%0:}\n&(gray)%1",
                            flp.username, String.join("\n", flp.notes));
                }
                break;

            case ADD:
                flp.notes.add(FLUtils.dateToString(System.currentTimeMillis(), "MM/dd/yyyy") + " " +
                        sender.getName() + ": " + joinArgsBeyond(1, " ", args));
                sendFormatted(sender, "&(gold)Note added.");
                break;

            case CLEAR:
                flp.notes.clear();
                sendFormatted(sender, "&(gold)Cleared notes of &(aqua)%0", flp.username);
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
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
