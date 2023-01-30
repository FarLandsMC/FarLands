package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.MessageFilter;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.ShareHome;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.kicas.rp.util.TextUtils.escapeExpression;

public class CommandSharehome extends Command {
    public CommandSharehome() {
        super(Rank.INITIATE, Category.HOMES, "Share a home for another player to add to their homes.",
                "/sharehome <send|accept|decline> <player> [home|name]", "sharehome");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        switch (args[0]) {
            case "send": // /sharehome send <home> <player> [message]
                if (!sendHome(sender, args)) {
                    error(sender, "Usage: /sharehome send <player> <home> [message]");
                }
                return true;
            case "accept":
                if (!acceptDeclineHome(true, sender, args)) {
                    error(sender, "Usage: /sharehome accept <player> [name]");
                }
                return true;
            case "decline":
                if (!acceptDeclineHome(false, sender, args)) {
                    error(sender, "Usage: /sharehome decline <player>");
                }
                return true;
            case "teleport":
                if(!teleportHome(sender, args)) {
                    error(sender, "Usage: /sharehome teleport <player>");
                }
            default:
                return false;
        }
    }

    // /sharehome send <player> <home> [message]
    private boolean sendHome(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return false;
        }

        // Get the recipient and make sure they exist
        OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
        if (recipientFlp == null) {
            error(sender, "Player not found.");
            return true;
        }
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Don't let people share homes with themselves
        if (flp.uuid.equals(recipientFlp.uuid)) {
            error(sender, "You cannot share a home with yourself.");
            return true;
        }

        // Make sure that the player has a home with this name
        if (!flp.hasHome(args[2])) {
            error(sender, "You don't have a home called " + args[2]);
            return true;
        }
        Home home = new Home(args[2], flp.getHome(args[2]));

        // If there is a message, apply colour codes
        final String message = FLUtils.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(2, " ", args)),
            escapedMessage = escapeExpression(message);

        if (recipientFlp.getIgnoreStatus(sender).includesSharehomes() || !recipientFlp.canAddHome()) {
            error(sender, "You cannot share a home with this person.");
            return true;
        }

        // Players can only queue one item at a time, so make sure this operation actually succeeds
        if (recipientFlp.addSharehome(flp.username, new ShareHome(flp.username, message.isEmpty() ? null : escapedMessage, home))) {
            // Use the same cooldown as /package
            success(sender, "Home shared!");
        } else { // The sender already has a sharehome queued for this person so the transfer failed
            error(sender, "You cannot share a home with %s right now.", recipientFlp.username);
        }

        return true;
    }

    // /sharehome <accept|decline> <player> [name]
    private boolean acceptDeclineHome(boolean accepted, CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        ShareHome shareHome = flp.pendingSharehomes.get(args[1]);

        if (shareHome == null) {
            return error(sender, "This player hasn't sent you a home.");
        }

        if (accepted) {
            if (!flp.canAddHome()) {
                return error(sender, "You cannot add this home as you have no more available homes.");
            }

            String homeName = shareHome.home().getName();
            if (args.length == 3) {
                // Make sure the home name is valid
                if (args[2].isEmpty() || args[2].matches("\\s+") || MessageFilter.INSTANCE.isProfane(args[2])) {
                    return error(sender, "You cannot set a home with that name.");
                }

                if (args[2].length() > 32) {
                    return error(sender, "Home names are limited to 32 characters. Please choose a different name.");
                }

                homeName = args[2];
            }
            if (flp.hasHome(homeName)) {
                return error(sender, "You already have a home by this name.");
            }
            flp.addHome(homeName, shareHome.home().asLocation());
            sender.sendMessage(
                ComponentColor.green("Home ")
                    .append(ComponentColor.aqua(homeName))
                    .append(ComponentColor.green(" added!"))
            );
        } else {
            success(sender, "Declined home sent by %s.", shareHome.sender());
        }
        flp.removeShareHome(shareHome.sender());
        return true;
    }

    private boolean teleportHome(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        if(!(sender instanceof Player)) return error(sender, "You must be in-game to run this command.");

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        ShareHome shareHome = flp.pendingSharehomes.get(args[1]);

        if (shareHome == null) {
            return error(sender, "This player hasn't sent you a home.");
        }

        FLUtils.tpPlayer((Player) sender, shareHome.home().getLocation());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        List<String> complete;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        switch(args.length) {
            case 1:
                complete = Arrays.asList("send", "accept", "decline", "teleport");
                break;

            case 2:
                if (args[0].equalsIgnoreCase("send")) {
                    return getOnlinePlayers(args[1], sender);
                }
                complete = new ArrayList<>(flp.pendingSharehomes.keySet());
                break;

            case 3:
                if (args[0].equalsIgnoreCase("send")) {
                    complete = flp.homes.stream().map(Home::getName).collect(Collectors.toList());
                } else {
                    return Collections.emptyList();
                }
                break;
            default:
                return Collections.emptyList();
        }
        return TabCompleterBase.filterStartingWith(args[args.length-1], complete);
    }


}
