package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.ShareHome;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.kicas.rp.util.TextUtils.escapeExpression;
import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandSharehome extends PlayerCommand {
    public CommandSharehome() {
        super(Rank.INITIATE, Category.HOMES, "Share a home for another player to add to their homes.",
                "/sharehome <send|accept|decline> <player> [home|name]", "sharehome");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        switch (args[0]) {
            case "send": // /sharehome send <home> <player> [message]
                if (!sendHome(sender, args)) {
                    sender.sendMessage(ChatColor.RED + "Usage: /sharehome send <player> <home> [message]");
                }
                return true;
            case "accept":
                if (!acceptDeclineHome(true, sender, args)) {
                    sender.sendMessage(ChatColor.RED + "Usage: /sharehome accept <player> [name]");
                }
                return true;
            case "decline":
                if (!acceptDeclineHome(false, sender, args)) {
                    sender.sendMessage(ChatColor.RED + "Usage: /sharehome decline <player>");
                }
                return true;
            default:
                return false;
        }
    }

    // /sharehome send <player> <home> [message]
    private boolean sendHome(Player sender, String[] args) {
        if (args.length < 3) {
            return false;
        }

        // Get the recipient and make sure they exist
        OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
        if (recipientFlp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        // Don't let people share homes with themselves
        if (sender.getUniqueId().equals(recipientFlp.uuid)) {
            sendFormatted(sender, "&(red)You cannot share a home with yourself.");
            return true;
        }

        // Make sure the sender has exhausted the command cooldown
        FLPlayerSession senderSession = FarLands.getDataHandler().getSession(sender);
        long timeRemaining = senderSession.commandCooldownTimeRemaining(this);
        if (timeRemaining > 0) {
            sendFormatted(sender, "&(red)You can share another home in %0",
                    TimeInterval.formatTime(50L * timeRemaining, false));
            return true;
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        // Make sure that the player has a home with this name
        if (!flp.hasHome(args[2])) {
            sender.sendMessage(ChatColor.RED + "You don't have a home called " + args[2]);
            return true;
        }
        Home home = new Home(args[2], flp.getHome(args[2]));

        // If there is a message, apply colour codes
        final String message = Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(2, " ", args)),
            escapedMessage = escapeExpression(message);

        if (recipientFlp.getIgnoreStatus(sender).includesSharehomes() || !recipientFlp.canAddHome()) {
            sender.sendMessage(ChatColor.RED + "You cannot share a home with this person.");
            return true;
        }

        // Players can only queue one item at a time, so make sure this operation actually succeeds
        if (recipientFlp.addSharehome(flp.username, new ShareHome(flp.username, message.isEmpty() ? null : escapedMessage, home))) {
            // Use the same cooldown as /package
            senderSession.setCommandCooldown(this, senderSession.handle.rank.getPackageCooldown() * 60L * 20L);
            sendFormatted(sender, "&(green)Home shared!");
        } else { // The sender already has a sharehome queued for this person so the transfer failed
            sendFormatted(sender, "&(red)You cannot share a home with %0 right now.", recipientFlp.username);
        }

        return true;
    }

    // /sharehome <accept|decline> <player> [name]
    private boolean acceptDeclineHome(boolean accepted, Player sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        ShareHome shareHome = flp.pendingSharehomes.get(args[1]);

        if (shareHome == null) {
            sender.sendMessage(ChatColor.RED + "This player hasn't sent you a home.");
            return true;
        }

        if (accepted) {
            if (!flp.canAddHome()) {
                sender.sendMessage(ChatColor.RED + "You cannot add this home as you have no more available homes.");
                return true;
            }

            String homeName = shareHome.home.getName();
            if (args.length == 3) {
                // Make sure the home name is valid
                if (args[2].isEmpty() || args[2].matches("\\s+") || Chat.getMessageFilter().isProfane(args[2])) {
                    sendFormatted(sender, "&(red)You cannot set a home with that name.");
                    return true;
                }

                if (args[2].length() > 32) {
                    sendFormatted(sender, "&(red)Home names are limited to 32 characters. Please choose a different name.");
                    return true;
                }

                homeName = args[2];
            }
            if (flp.hasHome(homeName)) {
                sender.sendMessage(ChatColor.RED + "You already have a home by this name.");
                return true;
            }
            flp.addHome(homeName, shareHome.home.asLocation());
            sendFormatted(sender, "&(green)Home {&(aqua)%0} added!", homeName);
        } else {
            sendFormatted(sender, "&(green)Declined home sent by %0", shareHome.sender);
        }
        flp.removeShareHome(shareHome.sender);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        List<String> complete;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        switch(args.length) {
            case 1:
                complete = Arrays.asList("send", "accept", "decline");
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
