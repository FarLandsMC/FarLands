package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.escapeExpression;
import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.MailMessage;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.FLUtils;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandMail extends Command {
    public CommandMail() {
        super(Rank.INITIATE, Category.CHAT, "Send a mail message to a player. Offline players will receive your " +
                "message when they log in.", "/mail <send|read|clear> [player|pageNumber] [message]", "mail");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            sendFormatted(sender, "&(red)You must be in-game to use this command.");
            return true;
        }

        // Get sender and action
        OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        Action action = Utils.valueOfFormattedName(args[0], Action.class);
        if (action == null) {
            sendFormatted(sender, "&(red)Invalid action \"%0\" expected one of the following: %1",
                    args[0], Arrays.stream(Action.VALUES).map(Utils::formattedName).collect(Collectors.joining(", ")));
            return true;
        }

        switch (action) {
            case SEND: {
                // Check arg count
                if (args.length == 1) {
                    sendFormatted(sender, "&(red)Usage: /mail send <player> [message]");
                    return true;
                }

                if (FarLands.getDataHandler().getOfflineFLPlayer(sender).isMuted()) {
                    sendFormatted(sender, "&(red)You cannot send mail while muted");
                    return true;
                }

                // Get recipient
                OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
                if (recipientFlp == null) {
                    sendFormatted(sender, "&(red)Player not found.");
                    return true;
                }

                if (args.length == 2) {
                    sendFormatted(sender, "&(red)You cannot send an empty mail message.");
                    return true;
                }

                // Prevent someone from spamming mail
                if (recipientFlp.mail.stream().filter(msg -> msg.sender().equals(sender.getName())).count() >= 5) {
                    sendFormatted(sender, "&(red)You cannot send any more mail to this person until they " +
                            "read your current messages and clear them.");
                    return true;
                }

                // Apply formatting
                String message = FLUtils.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(1, " ", args));
                if (!senderFlp.rank.isStaff())
                    message = escapeExpression(message);
                sendMailMessage(sender, "To", recipientFlp.rank.getNameColor(),
                    recipientFlp.username, message);

                // Check for ignoring
                if (!recipientFlp.getIgnoreStatus(senderFlp).includesChat())
                    recipientFlp.addMail(sender.getName(), message);
                break;
            }

            case READ: {
                // Empty mailbox
                if (senderFlp.mail.isEmpty()) {
                    sendFormatted(sender, "&(gold)You have no mail.");
                    return true;
                }

                // Try to parse the page number if it exists
                int index;
                try {
                    // Multiply by five since we show five messages per page
                    index = args.length == 1 ? 0 : (Integer.parseInt(args[1]) - 1) * 5;
                } catch (NumberFormatException ex) {
                    index = -1;
                }

                // Bad page index
                if (index < 0) {
                    sendFormatted(sender, "&(red)Invalid page number: %0", args[1]);
                    return true;
                }
                // Too large of an index
                else if (index >= senderFlp.mail.size()) {
                    sendFormatted(sender, "&(red)You do not have enough mail to fill that page.");
                    return true;
                }

                MailMessage message;
                for (int i = index; i < Math.min(index + 5, senderFlp.mail.size()); ++i) {
                    message = senderFlp.mail.get(i);
                    sendMailMessage(sender, "From", ChatColor.GOLD, message.sender(), message.message());
                }

                sendFormatted(sender, "&(gold)Clear your mail with $(hovercmd,/mail clear,{&(gray)Click to Run},&(yellow)/mail clear)");
                break;
            }

            case CLEAR: {
                senderFlp.mail.clear();
                sendFormatted(sender, "&(green)Mail cleared.");
                break;
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length <= 1) {
            return Arrays.stream(Action.VALUES).map(Utils::formattedName)
                    .filter(action -> action.startsWith(args.length == 0 ? "" : args[0]))
                    .collect(Collectors.toList());
        } else if (Utils.valueOfFormattedName(args[0], Action.class) == Action.SEND && args.length <= 2)
            return getOnlinePlayers(args[1], sender);
        else
            return Collections.emptyList();
    }

    private static void sendMailMessage(CommandSender recipient, String prefix, ChatColor color, String name, String message) {
        sendFormatted(recipient, "&(dark_gray)%0 %1%2: &(reset)%3", prefix, color, name, message);
    }

    private enum Action {
        SEND, READ, CLEAR;

        static final Action[] VALUES = values();
    }
}
