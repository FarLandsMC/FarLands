package net.farlands.sanctuary.command.player;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.MiniMessageWrapper;
import net.farlands.sanctuary.chat.Pagination;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
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
        if (args.length == 0) {
            return false;
        }

        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            return error(sender, "You must be in-game to use this command.");
        }

        // Get sender and action
        OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        Action action = Utils.valueOfFormattedName(args[0], Action.class);
        if (action == null) {
            return error(
                sender,
                "Invalid action \"%s\" expected one of the following: %s",
                args[0],
                Arrays.stream(Action.VALUES).map(Utils::formattedName).collect(Collectors.joining(", "))
            );
        }

        switch (action) {
            case SEND -> {
                // Check arg count
                if (args.length == 1) {
                    return error(sender, "Usage: /mail send <player> [message]");
                }

                if (FarLands.getDataHandler().getOfflineFLPlayer(sender).isMuted()) {
                    return error(sender, "You cannot send mail while muted.");
                }

                // Get recipient
                OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
                if (recipientFlp == null) {
                    return error(sender, "Player not found.");
                }

                if (args.length == 2) {
                    return error(sender, "You cannot send an empty mail message.");
                }

                // Prevent someone from spamming mail
                if (recipientFlp.mail.stream().filter(msg -> msg.sender().equals(sender.getName())).count() >= 5) {
                    return error(sender, "You cannot send any more mail to this person until they " +
                                         "read your current messages and clear them.");
                }

                // Apply formatting
                Component message = MiniMessageWrapper.farlands(senderFlp).mmParse(joinArgsBeyond(1, " ", args));
                sendMailMessage(sender, "To", recipientFlp, message);

                // Check for ignoring
                if (!recipientFlp.getIgnoreStatus(senderFlp).includesChat()) {
                    recipientFlp.addMail(sender.getName(), message);
                }
            }
            case READ -> {
                // Empty mailbox
                if (senderFlp.mail.isEmpty()) {
                    return info(sender, "You have no mail.");
                }

                // Try to parse the page number if it exists
                int index;
                try {
                    // Multiply by five since we show five messages per page
                    index = args.length == 1 ? 1 : Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    index = -1;
                }

                Pagination pagination = new Pagination(ComponentColor.gold("Mail"), "/mail read");
                pagination.addLines(ComponentLike.asComponents(senderFlp.mail));

                int pages = pagination.numPages();
                sender.sendMessage("Pages: " + pages);

                if (index < 1 || index > pages) {
                    return error(sender, "Invalid page number: %s", args.length == 1 ? 1 : args[1]);
                }

                pagination.sendPage(index, sender);

                info(sender, "Clear your mail with /mail clear");
            }
            case CLEAR -> {
                int size = senderFlp.mail.size();
                senderFlp.mail.clear();
                return success(sender, "Cleared %d message%s", size, size == 1 ? "" : "s");
            }
        }

        return true;
    }

    private void sendMailMessage(CommandSender sender, String prefix, OfflineFLPlayer flp, Component message) {
        sender.sendMessage(
            Component.empty()
                .append( // <To|From> <flp name>:
                    ComponentColor.darkGray(prefix)
                        .append(flp.getFullDisplayName(true))
                        .append(Component.text(": "))
                )
                .append(message) // <message>
        );
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length <= 1) {
            return Arrays.stream(Action.VALUES).map(Utils::formattedName)
                .filter(action -> action.startsWith(args.length == 0 ? "" : args[0]))
                .collect(Collectors.toList());
        } else if (Utils.valueOfFormattedName(args[0], Action.class) == Action.SEND && args.length <= 2) {
            return getOnlinePlayers(args[1], sender);
        } else {
            return Collections.emptyList();
        }
    }

    private enum Action {
        SEND, READ, CLEAR;

        static final Action[] VALUES = values();
    }
}
