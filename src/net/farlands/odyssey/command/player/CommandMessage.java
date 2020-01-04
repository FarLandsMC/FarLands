package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TextUtils;

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
    public CommandMessage() {
        super(Rank.INITIATE, "Send a private message to another player.", "/msg <player> <message>", true, "msg",
                "w", "m", "r", "tell", "whisper");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession senderSession = FarLands.getDataHandler().getSession(sender);
        // Reply to the last message sent
        if ("r".equals(args[0])) {
            if (args.length < 2)
                return true;
            CommandSender recipient = senderSession.lastMessageSender.getValue();
            if (recipient == null) {
                sender.sendMessage(ChatColor.RED + "You have no recent messages to reply to.");
                return true;
            }
            // Keep the name stored
            sendMessage(recipient, sender, Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(0, " ", args)));
        }
        // Non-/r aliases
        else {
            // No arguments sent to the command
            if (args.length == 1) {
                CommandSender currentReplyToggle = senderSession.replyToggleRecipient;
                CommandSender lastMessageSender = senderSession.lastMessageSender.getValue();
                if (currentReplyToggle == null) {
                    if (lastMessageSender == null)
                        sender.sendMessage(ChatColor.RED + "You do not have an active reply toggle currently.");
                    else {
                        senderSession.replyToggleRecipient = lastMessageSender;
                        sendFormatted(sender, "&(gold)You are now messaging {&(aqua)%0}. Type " +
                                "$(hovercmd,/m,{&(gray)Click to Run},&(aqua)/m) to toggle off, " +
                                "or start your message with {&(aqua)!} to send it to public chat.", lastMessageSender.getName());
                    }
                } else {
                    senderSession.replyToggleRecipient = null;
                    sender.sendMessage(ChatColor.GOLD + "You are no longer messaging " + currentReplyToggle.getName() + ".");
                }
                return true;
            } else if (args.length == 2) {
                Player newToggled = getPlayer(args[1], sender);
                if (newToggled == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found");
                    return true;
                }
                CommandSender toggled = senderSession.replyToggleRecipient;
                if (toggled != null && toggled.equals(newToggled)) {
                    senderSession.replyToggleRecipient = null;
                    sender.sendMessage(ChatColor.GOLD + "You are no longer messaging " + toggled.getName() + ".");
                } else {
                    senderSession.replyToggleRecipient = newToggled;
                    sendFormatted(sender, "&(gold)You are now messaging {&(aqua)%0}" +
                            (toggled == null ? "" : " and no longer messaging {&(aqua)" + toggled.getName() + "}") +
                            ". Type $(hovercmd,/m,{&(gray)Click to Run},&(aqua)/m) to toggle off, " +
                            "or start your message with {&(aqua)!} to send it to public chat.", newToggled.getName());
                }
                return true;
            }
            CommandSender recipient = getPlayer(args[1], sender);
            if (recipient == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            // Try to send the message, and if it succeeds then store the metadata for /r
            sendMessage(recipient, sender, Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(1, " ", args)));
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
    public static void sendMessage(CommandSender recipient, CommandSender sender, String message) {
        OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayer(recipient);
        // Censor the message if censoring
        if (recipientFlp != null && recipientFlp.isCensoring())
            message = Chat.getMessageFilter().censor(message);
        // This changes the message for the sender so they can see their message was censored when sent

        if (sender instanceof Player)
            sender.sendMessage(format("To", getRank(recipient), getDisplayName(recipient), message));
        recipient.sendMessage(format("From", getRank(sender), getDisplayName(sender), message));
        FLPlayerSession recipientSession = null;
        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            recipientSession = FarLands.getDataHandler().getSession(player);
            // Check for AFK toggle
            if (recipientSession.afk)
                sender.sendMessage(ChatColor.RED + "This player is AFK, so they may not receive your message.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
        }
        if (recipientSession != null)
            recipientSession.lastMessageSender.setValue(sender, 10L * 60L * 20L, null);
        if (sender instanceof Player)
            FarLands.getDataHandler().getSession((Player) sender).lastMessageSender.setValue(recipient, 10L * 60L * 20L, null);
        String senderName = sender instanceof Player ? sender.getName() : sender.getName();
        String recipientName = recipient instanceof Player ? recipient.getName() : recipient.getName();
        Chat.broadcastStaff(TextUtils.format("&(red)[%0 -> %1]: &(gray)%2", senderName, recipientName, message));
    }

    private static String format(String prefix, Rank rank, String name, String message) {
        return ChatColor.DARK_GRAY + prefix + ' ' + rank.getNameColor() + name + ": " + ChatColor.RESET + message;
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
