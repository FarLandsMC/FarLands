package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatHandler;
import net.farlands.sanctuary.chat.MessageFilter;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
            if (args.length == 1) {
                return true;
            }

            // Check to make sure they have a recent conversation to reply to
            CommandSender recipient = senderSession.lastMessageSender.getValue();
            if (recipient == null) {
                sender.sendMessage(ComponentColor.red("You have no recent messages to reply to."));
                return true;
            }

            OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayer(recipient);
            OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

            // Keep the name stored
            sendMessages(recipientFlp, senderFlp, joinArgsBeyond(0, " ", args));
        } else { // Non-/r aliases
            // No arguments sent to the command so we toggle auto-messaging
            if (args.length == 1) {
                CommandSender currentReplyToggle = senderSession.replyToggleRecipient;
                CommandSender lastMessageSender = senderSession.lastMessageSender.getValue();

                // The sender does not have the toggle currently active for anyone
                if (currentReplyToggle == null) {
                    // They do not have a recent conversation, so we don't know who to set the reply toggle to
                    if (lastMessageSender == null) {
                        sender.sendMessage(ComponentColor.red("You do not have an active reply toggle currently."));
                    }
                    // Set the recipient to the person they were last chatting with
                    else {
                        senderSession.replyToggleRecipient = lastMessageSender;
                        Component c = Component.text()
                            .content("You are now messaging ")
                            .color(NamedTextColor.GOLD)
                            .append(ComponentColor.aqua(lastMessageSender.getName()))
                            .append(ComponentColor.gold(". Type "))
                            .append(ComponentUtils.command("/m"))
                            .append(ComponentColor.gold(" to toggle off, or start your message with "))
                            .append(ComponentColor.aqua("!"))
                            .append(ComponentColor.gold(" to send it to public chat."))
                            .build();
                        sender.sendMessage(c);
                    }
                }
                // Disable auto-messaging since it's already active
                else {
                    senderSession.replyToggleRecipient = null;
                    sender.sendMessage(ComponentColor.gold("You are no longer messaging " + currentReplyToggle.getName()));
                }

                return true;
            }

            // Get the recipient and make sure they exist
            CommandSender recipient = getPlayer(args[1], sender);
            if (recipient == null) {
                sender.sendMessage(ComponentColor.red("Player not found."));
                return true;
            }

            // One argument was sent, so toggle auto-reply for the player they specified
            if (args.length == 2) {
                CommandSender toggled = senderSession.replyToggleRecipient;

                // The name the specified matches who they are currently replying to, so disable auto-reply
                if (toggled != null && toggled.equals(recipient)) {
                    senderSession.replyToggleRecipient = null;
                    sender.sendMessage(ComponentColor.gold("You are no longer messaging " + toggled.getName()));
                }
                // They specified a different person so switch to the new player
                else {
                    senderSession.replyToggleRecipient = recipient;

                    TextComponent.Builder c = Component.text()
                        .content("You are now messaging ")
                        .color(NamedTextColor.GOLD)
                        .append(ComponentColor.aqua(recipient.getName()))
                        .append(ComponentColor.gold(". Type "));
                    if (toggled != null) {
                        c.append(Component.text(" and no longer messaging "))
                            .append(ComponentColor.aqua(toggled.getName()));
                    }
                    c.append(ComponentUtils.command("/m"))
                        .append(ComponentColor.gold(" to toggle off, or start your message with "))
                        .append(ComponentColor.aqua("!"))
                        .append(ComponentColor.gold(" to send it to public chat."));
                    sender.sendMessage(c.build());
                }

                return true;
            }

            OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayer(recipient);
            OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            // Try to send the message, and if it succeeds then store the metadata for /r
            sendMessages(recipientFlp, senderFlp, joinArgsBeyond(1, " ", args));
        }
        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender ||
            !FarLands.getDataHandler().getOfflineFLPlayer(sender).isMuted())) {
            sender.sendMessage(ComponentColor.red("You cannot use this command while muted."));
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
    // TODO: 1/3/22 Change this method to accept components
    public static void sendMessages(OfflineFLPlayer recipientFlp, OfflineFLPlayer senderFlp, String messageStr) {
        Player recipientPlayer = recipientFlp.getOnlinePlayer();
        Player senderPlayer = senderFlp.getOnlinePlayer();

        // Censor the message if censoring
        if (recipientFlp.censoring) {
            messageStr = MessageFilter.INSTANCE.censor(messageStr);
        }

        Component message = ChatHandler.handleReplacements(messageStr, senderFlp);

        // Send the messages to both parties involved
        sendMessage(senderFlp, true, recipientFlp.rank, recipientFlp.getDisplayName(), message);
        sendMessage(recipientFlp, false, senderFlp.rank, senderFlp.getDisplayName(), message);

        // Find the recipient's session if it exists
        FLPlayerSession recipientSession = recipientFlp.getSession();

        // Check for AFK toggle
        if (recipientSession.afk) {
            senderPlayer.sendMessage(ComponentColor.red("This player is AFK, so they may not receive your message."));
        }

        // Play a sound for the recipient if they're online to notify them of the message
        recipientPlayer.playSound(recipientPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);

        // Store the sender for easy replying
        recipientSession.lastMessageSender.setValue(senderPlayer, 10L * 60L * 20L, null);


        // Store the recipient for easy replying
        FarLands.getDataHandler().getSession(senderPlayer).lastMessageSender.setValue(recipientPlayer, 10L * 60L * 20L, null);

        // Notify staff of the message
        Logging.broadcastStaffExempt(
            ComponentColor.red("[%s ??? %s]: ", senderFlp.username, recipientFlp.username)
                .append(ComponentColor.gray(ComponentUtils.toText(message))), // Remove color/formatting
            senderPlayer, recipientPlayer
        );
    }

    private static void sendMessage(OfflineFLPlayer recipient, boolean toMessage, Rank rank, Component name, Component message) {
        Component finalC = ComponentColor.darkGray(toMessage ? "To" : "From")
                .append(Component.space())
                .append(name)
                .append(Component.text(": ").color(rank.nameColor()))
                .append(message.color(NamedTextColor.WHITE));
        recipient.getOnlinePlayer().sendMessage(finalC);
        if(!toMessage) { // add to "While you were gone" message
            recipient.getSession().addAFKMessage(finalC);
        }
    }
}
