package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.escapeExpression;
import static com.kicas.rp.util.TextUtils.sendFormatted;
import static com.kicas.rp.util.TextUtils.format;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.Logging;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandMessage extends PlayerCommand {
    private static final String REPLY_ALIAS = "r";

    public CommandMessage() {
        super(Rank.INITIATE, Category.CHAT, "Send a private message to another player, reply to a conversation (/r), or toggle auto-messaging (/m <player>).",
                "/msg <player> <message>", true, "msg", "w", "m", REPLY_ALIAS, "tell", "whisper");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession senderSession = FarLands.getDataHandler().getSession(sender);

        // Reply to the last message sent
        if (REPLY_ALIAS.equals(args[0])) {
            // If they just type "/r" ignore it
            if (args.length == 1)
                return true;

            // Check to make sure they have a recent conversation to reply to
            CommandSender recipient = senderSession.lastMessageSender.getValue();
            if (recipient == null) {
                sendFormatted(sender, "&(red)You have no recent messages to reply to.");
                return true;
            }

            // Keep the name stored
            sendMessages(recipient, sender, Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(0, " ", args)));
        }
        // Non-/r aliases
        else {
            // No arguments sent to the command so we toggle auto-messaging
            if (args.length == 1) {
                CommandSender currentReplyToggle = senderSession.replyToggleRecipient;
                CommandSender lastMessageSender = senderSession.lastMessageSender.getValue();

                // The sender does not have the toggle currently active for anyone
                if (currentReplyToggle == null) {
                    // They do not have a recent conversation, so we don't know who to set the reply toggle to
                    if (lastMessageSender == null)
                        sendFormatted(sender, "&(red)You do not have an active reply toggle currently.");
                    // Set the recipient to the person they were last chatting with
                    else {
                        senderSession.replyToggleRecipient = lastMessageSender;
                        sendFormatted(sender, "&(gold)You are now messaging {&(aqua)%0}. Type " +
                                "$(hovercmd,/m,{&(gray)Click to Run},&(aqua)/m) to toggle off, " +
                                "or start your message with {&(aqua)!} to send it to public chat.",
                                lastMessageSender.getName());
                    }
                }
                // Disable auto-messaging since it's already active
                else {
                    senderSession.replyToggleRecipient = null;
                    sendFormatted(sender, "&(gold)You are no longer messaging %0", currentReplyToggle.getName());
                }

                return true;
            }

            // Get the recipient and make sure they exist
            CommandSender recipient = getPlayer(args[1], sender);
            if (recipient == null) {
                sendFormatted(sender, "&(red)Player not found.");
                return true;
            }

            // One argument was sent, so toggle auto-reply for the player they specified
            if (args.length == 2) {
                CommandSender toggled = senderSession.replyToggleRecipient;

                // The name the specified matches who they are currently replying to, so disable auto-reply
                if (toggled != null && toggled.equals(recipient)) {
                    senderSession.replyToggleRecipient = null;
                    sendFormatted(sender, "&(gold)You are no longer messaging %0", toggled.getName());
                }
                // They specified a different person so switch to the new player
                else {
                    senderSession.replyToggleRecipient = recipient;
                    sendFormatted(sender, "&(gold)You are now messaging {&(aqua)%0}" +
                            (toggled == null ? "" : " and no longer messaging {&(aqua)" + toggled.getName() + "}") +
                            ". Type $(hovercmd,/m,{&(gray)Click to Run},&(aqua)/m) to toggle off, " +
                            "or start your message with {&(aqua)!} to send it to public chat.", recipient.getName());
                }

                return true;
            }

            // Try to send the message, and if it succeeds then store the metadata for /r
            sendMessages(recipient, sender, Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(1, " ", args)));
        }
        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender ||
                !FarLands.getDataHandler().getOfflineFLPlayer(sender).isMuted())) {
            sender.sendMessage(ChatColor.RED + "You cannot use this command while muted.");
            return false;
        }
        return super.canUse(sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && !"r".equals(alias)
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }

    // Send the formatted message
    public static void sendMessages(CommandSender recipient, CommandSender sender, String message) {
        if (!FarLands.getDataHandler().getOfflineFLPlayer(sender).rank.isStaff())
            message = escapeExpression(message);

        OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayer(recipient);
        // Censor the message if censoring
        String censored = message;
        if (recipientFlp != null && recipientFlp.censoring)
            censored = Chat.getMessageFilter().censor(message);

        // Send the messages to both parties involved
        if (sender instanceof Player)
            sendMessage(sender, "To", getRank(recipient), getDisplayName(recipient), message);
        sendMessage(recipient, "From", getRank(sender), getDisplayName(sender), censored);

        // Find the recipient's session if it exists
        FLPlayerSession recipientSession;
        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            recipientSession = FarLands.getDataHandler().getSession(player);

            // Check for AFK toggle
            if (recipientSession.afk)
                sendFormatted(sender, "&(red)This player is AFK, so they may not receive your message.");

            // Play a sound for the recipient if they're online to notify them of the message
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);

            // Store the sender for easy replying
            recipientSession.lastMessageSender.setValue(sender, 10L * 60L * 20L, null);
        }

        // Store the recipient for easy replying
        if (sender instanceof Player)
            FarLands.getDataHandler().getSession((Player) sender).lastMessageSender.setValue(recipient, 10L * 60L * 20L, null);

        // Notify staff of the message
        Logging.broadcastStaff(format("&(red)[%0 -> %1]: &(gray)%2", Chat.removeColorCodes(sender.getName()),
                Chat.removeColorCodes(recipient.getName()), message));
    }

    private static void sendMessage(CommandSender recipient, String prefix, Rank rank, String name, String message) {
        sendFormatted(recipient, "&(dark_gray)%0 &(gray)%1%2: &(reset)%3", prefix, rank.getNameColor(), escapeExpression(name), message);
    }

    private static String getDisplayName(CommandSender sender) {
        if (sender instanceof Player)
            return FarLands.getDataHandler().getOfflineFLPlayer(sender).getDisplayName();

        return sender.getName();
    }

    private static Rank getRank(CommandSender sender) {
        return sender instanceof Player || sender instanceof DiscordSender ? Rank.getRank(sender) : Rank.INITIATE;
    }
}
