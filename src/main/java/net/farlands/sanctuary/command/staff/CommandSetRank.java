package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.AFK;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSetRank extends Command {
    public CommandSetRank() {
        super(Rank.BUILDER, "Sets the rank of a player.", "/setrank <player> <rank>", "setrank");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            error(sender, "Could not find player: {}", args[0]);
            return true;
        }
        Rank rank = FLUtils.safeValueOf(Rank::valueOf, args[1].toUpperCase());
        if (rank == null) {
            error(sender, "Invalid rank: {}", args[1]);
            return true;
        }
        // You cannot modify someone of an equal rank, and you cannot set someone to a higher rank than yours
        if ((flp.rank.specialCompareTo(Rank.getRank(sender)) >= 0 || rank.specialCompareTo(Rank.getRank(sender)) > 0) &&
                !(sender instanceof ConsoleCommandSender)) {
            error(sender, "You do not have permission to set {} to rank {}.", flp, rank);
            return true;
        }

        flp.setRank(rank);

        // Manage all the toggles and stuff that will change with rank
        FLPlayerSession session = flp.getSession();
        if (session != null)
            AFK.setAFKCooldown(session.player);

        success(sender, "Updated {}'s rank to {}", flp, rank);
        Player player = flp.getOnlinePlayer();
        if (player != null) // Notify the player if they're online
            success(player, "Your rank has been updated to {}", rank);
        // Notify discord
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, MarkdownProcessor.escapeMarkdown(sender.getName()) +
                " has updated " + MarkdownProcessor.escapeMarkdown(flp.username) + "'s rank to `" + rank.getName() +
                "`.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        switch (args.length) {
            case 0:
            case 1:
                return getOnlinePlayers(args.length == 0 ? "" : args[0], sender);
            case 2:
                return Arrays.stream(Rank.VALUES).map(Rank::toString)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }
}
