package net.farlands.odyssey.command.discord;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandNotes extends DiscordCommand {
    public CommandNotes() {
        super(Rank.JR_BUILDER, "View, clear, or add notes to a player.", "/notes <view|add|clear> <player> add?[note]", "notes");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
        if (flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if ("view".equals(args[0])) {
            if (flp.notes.isEmpty())
                sender.sendMessage(ChatColor.AQUA + flp.getUsername() + ChatColor.GOLD + " does not have any notes.");
            else {
                sender.sendMessage(ChatColor.GOLD + "Showing notes for " + ChatColor.AQUA + flp.getUsername() + ":");
                flp.notes.forEach(note -> sender.sendMessage(ChatColor.GRAY + note));
            }
        } else if ("add".equals(args[0])) {
            flp.notes.add(Utils.dateToString(System.currentTimeMillis(), "MM/dd/yyyy") + " " +
                    sender.getName() + ": " + joinArgsBeyond(1, " ", args));
            sender.sendMessage(ChatColor.GOLD + "Note added.");
        } else if ("clear".equals(args[0])) {
            flp.notes.clear();
            sender.sendMessage(ChatColor.GOLD + "Cleared notes of " + ChatColor.AQUA + flp.getUsername());
        } else
            return false;
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length <= 1) {
            return Stream.of("view", "add", "clear").filter(action -> action.startsWith(args.length == 0 ? "" : args[0]))
                    .collect(Collectors.toList());
        } else if (args.length == 2)
            return getOnlinePlayers(args[1], sender);
        else
            return Collections.emptyList();
    }
}
