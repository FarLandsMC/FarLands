package net.farlands.sanctuary.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandMessage;
import net.farlands.sanctuary.command.player.CommandNick;
import net.farlands.sanctuary.command.player.CommandStats;
import net.farlands.sanctuary.command.staff.CommandStaffChat;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * A central class for handling all chat features
 */
public class ChatHandler {

    public static void playerLog(PlayerEvent event) {
        boolean join = event instanceof PlayerJoinEvent;
        if (join) { // Reset message
            ((PlayerJoinEvent) event).joinMessage(null);
        } else {
            ((PlayerQuitEvent) event).quitMessage(null);
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        if (flp.vanished) {
            Logging.broadcastStaff(
                ComponentColor.yellow(
                    "%s has %s silently.",
                    event.getPlayer().getName(),
                    join ? "joined" : "left"
                ),
                DiscordChannel.STAFF_COMMANDS
            );
        } else {
            playerTransition(flp, join);
        }

    }

    public static void playerTransition(OfflineFLPlayer flp, boolean join) {
        String joinOrLeave = join ? "joined" : "left";
        Component message = Component.text() // > <player> has <joined/left>.
            .color(NamedTextColor.YELLOW)
            .append(
                Component.text(" > ").style(Style.style(TextDecoration.BOLD))
            )
            .append(flp.rank.colorName(flp.username))
            .append(ComponentColor.yellow(" has %s.", joinOrLeave))
            .build();
        Bukkit.broadcast(message);
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, message);

    }

    public static void onChat(AsyncChatEvent event) {
        if (event.isCancelled()) return; //  If something cancels the event, don't do anything.
        event.setCancelled(true);
        Player player = event.getPlayer();
        OfflineFLPlayer sender = FarLands.getDataHandler().getOfflineFLPlayer(player);
        String message = ComponentUtils.toText(event.message());

        message = ChatControl.limitFlood(message); // Restrict flooding characters
        message = ChatControl.limitCaps(message); // Restrict capital characters

        if (handleMute(player, sender, message)) return; // If muted, send messages and stop anything else

        if (handleSpam(player, sender, message)) return;

        if (handleCensor(player, sender, message)) return; // If censored, send messages and stop anything else

        // Message starts with "!" and sender isn't vanished
        boolean shout = message.startsWith("!") && !sender.vanished;

        if (shout) {
            if (message.length() <= 1) { // Message is just "!"
                return;
            }
            message = message.substring(1);
        }

        message = handleReplacements(message, sender);

        if (!shout) {
            FLPlayerSession session = FarLands.getDataHandler().getSession(player);
            if (sender.vanished || session.autoSendStaffChat) {
                staffChat(player, message);
                return;
            }

            if (session.replyToggleRecipient != null) {
                if (session.replyToggleRecipient instanceof Player && ((Player) session.replyToggleRecipient).isOnline()) {
                    // TODO: Rewrite CommandMessage#sendMessages to use Components
                    FarLands
                        .getCommandHandler()
                        .getCommand(CommandMessage.class)
                        .execute(player, new String[]{ "r", message });
                    return;
                }
            }
        }
        chat(sender, message);

    }

    public static boolean handleSpam(Player player, String message) {
        return handleSpam(player, FarLands.getDataHandler().getOfflineFLPlayer(player), message);
    }

    public static boolean handleSpam(Player player, OfflineFLPlayer sender, String message) {
        if (sender.rank.isStaff()) return false;

        FLPlayerSession session = FarLands.getDataHandler().getSession(player);
        double strikes = session.spamAccumulation;

        if (FLUtils.deltaEquals(strikes, 0.0, 1e-8)) {
            session.spamCooldown.reset(() -> session.spamAccumulation = 0.0);
        }

        strikes += 1 + message.length() / 80.0;

        if (strikes >= 7.0) {
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                player.kick(ComponentColor.red("Kicked for spam. Repeating this offense could result in a ban."));
                AntiCheat.broadcast(player.getName() + " was kicked for spam.", true);
            });
            return true;
        }

        return false;
    }

    public static void staffChat(Player player, String message) {
        // Run `/c <message>` to send to staff chat
        FarLands
            .getCommandHandler()
            .getCommand(CommandStaffChat.class)
            .execute(player, new String[]{ "c", message });
    }


    /**
     * Send chat message as OfflineFLPlayer
     *
     * @param sender  Sender of chat message
     * @param prefix  Sender's prefix
     * @param message Message Text
     */
    public static void chat(OfflineFLPlayer sender, Component prefix, String message) {
        Component component = MiniMessage.miniMessage().parse(message);
        Component censorComponent = MiniMessage.miniMessage().parse(MessageFilter.INSTANCE.censor(message));

        sendToConsole(component, sender);
        broadcast(prefix, component, censorComponent, sender); // Send to players
        broadcastDiscord(prefix, component); // Send to #in-game
    }

    public static Component getPrefix(OfflineFLPlayer sender) {
        // Does the player have permission to have a nickname? - Based on `/nick` permission
        boolean useNick = sender.rank
            .specialCompareTo(
                FarLands.getCommandHandler()
                    .getCommand(CommandNick.class)
                    .getMinRankRequirement()
            ) > 0;

        Component rankPrefix = sender.rank.getLabel(); // Ex: "Dev", "Knight" (with color and bold?)
        Component nameDisplay = sender.rank.colorName(
                useNick && sender.nickname != null && !sender.nickname.isBlank() ?
                    sender.nickname :
                    sender.username
            ) // Colored Username or Nickname
            .hoverEvent(
                HoverEvent.showText(
                    CommandStats.getFormattedStats(sender, false)
                )
            ) // Hover Stats
            .clickEvent(
                ClickEvent.suggestCommand(
                    "/msg " + sender.username + " "
                )
            ); // Click Command Suggestion

        return Component.text() // Prefix to all messages
            .append(rankPrefix) // "<rank>"
            .append(Component.text(' ')) // "<rank> "
            .append(nameDisplay) // "<rank> <name>"
            .append(Component.text(": ").color(sender.rank.color())) // "<rank> <name>: "
            .build();
    }

    /**
     * Send chat message as OfflineFLPlayer
     *
     * @param sender  Sender of chat message
     * @param message Message Text
     */
    public static void chat(OfflineFLPlayer sender, String message) {
        chat(sender, getPrefix(sender), message);
    }

    private static String handleReplacements(String message, OfflineFLPlayer sender) {

        message = message.replaceAll("<", "\\\\<"); // Protect all potential codes from influencing chat

        message = ChatFormat.translateColors(message, sender); // &a -> <green> | &#abc -> <#aabbcc> | &#facade -> <#facade>
        message = ChatFormat.translateEmotes(message); // :shrug: -> ¯\_(ツ)_/¯
        message = ChatFormat.translateLinks(message); // Highlight Links
        message = ChatFormat.translatePing(message, sender, false); // Make @<name> have hover text with stats
        message = ChatFormat.translateCommands(message); // `cmd` -> cmd (with hover and click event)
        message = ChatFormat.translateItem(message, sender.getOnlinePlayer()); // [i] -> "[i] <name>" with hover text

        return message;
    }

    private static boolean handleCensor(Player player, OfflineFLPlayer sender, String message) {
        if (sender.rank.isStaff()) return false;
        if (MessageFilter.INSTANCE.autoCensor(message)) {
            boolean alertPlayer = sender.secondsPlayed < 60 * 15;
            if (alertPlayer) { // Less than 15 minutes of playtime
                player.sendMessage(
                    ComponentColor.red(
                        "Your message has not been sent, because it may contain messages or phrases offensive to some"
                    )
                );
            } else { // Send a false message
                player.sendMessage(
                    ComponentColor.white("")
                        .append(getPrefix(sender))
                        .append(ComponentColor.white(message))
                );
            }
            Logging.broadcastStaff(
                String.format(
                    ChatColor.RED + "[AUTO-CENSOR] %s: " + ChatColor.GRAY + "%s - " + ChatColor.RED + "%s",
                    sender.getDisplayName(),
                    message,
                    alertPlayer ? "Notified player." : "False message sent to player."
                ),
                DiscordChannel.ALERTS
            );
            return true;
        }
        return false;

    }

    private static boolean handleMute(Player player, OfflineFLPlayer flp, String message) {
        if (!flp.isMuted()) return false;
        flp.currentMute.sendMuteMessage(player);
        Logging.broadcastStaff(
            ComponentColor.red("[MUTED] %s: ", flp.username)
                .append(ComponentColor.gray(message))
        );
        return true;
    }

    public static void autoCensorAlert(Player sender, Component message, OfflineFLPlayer flp) {
        boolean fakeMsg = flp.secondsPlayed < 60 * 15; // Played for more than 15 minutes
        if (fakeMsg) {
            // Make it seem like the message went through for the sender
            broadcast(message, flp, sender);
        } else {
            // Let the sender know that their message wasn't sent
            sender.sendMessage(
                ComponentColor.red(
                    "Your message was not sent as it may have contained words or phrases that some may find offensive."
                )
            );
        }
        Logging.broadcastStaff(
            ComponentColor.red("[AUTO-CENSOR] %s: ", flp.username)
                .append(message.color(NamedTextColor.GRAY))
                .append(ComponentColor.red(" - %s player.", fakeMsg ? "False message sent to" : "Notified")),
            DiscordChannel.ALERTS
        );
    }

    public static void broadcastDiscord(Component prefix, Component messageC) {
        String prefixStr = MarkdownProcessor.escapeMarkdown(ComponentUtils.toText(prefix));
        String message = MarkdownProcessor.fromMinecraft(messageC);
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.IN_GAME, prefixStr + message);
    }

    /**
     * Send message only to specific players
     *
     * @param message   The message to send
     * @param sender    The sender
     * @param selection The selected players
     */
    public static void broadcast(Component message, OfflineFLPlayer sender, Player... selection) {
        for (Player player : selection) {
            player.sendMessage(message);
        }
    }

    public static void broadcast(Component message, OfflineFLPlayer sender) {
        Bukkit.getOnlinePlayers()
            .stream()
            .filter(p -> !FarLands.getDataHandler().getOfflineFLPlayer(p).getIgnoreStatus(sender).includesChat())
            .forEach(p -> p.sendMessage(message));
    }

    public static void broadcast(Component prefix, Component message, Component censorMessage, OfflineFLPlayer sender) {
        Bukkit.getOnlinePlayers()
            .stream()
            .map(FarLands.getDataHandler()::getOfflineFLPlayer)
            .filter(flp -> !flp.getIgnoreStatus(sender).includesChat())
            .forEach(flp -> {
                flp.getOnlinePlayer().sendMessage(
                    Component.text("")
                        .append(prefix)
                        .append(flp.censoring ? censorMessage : message)
                );
            });
    }

    public static void sendToConsole(Component message, OfflineFLPlayer sender) {
        Bukkit.getConsoleSender().sendMessage(
            sender.rank.getLabel()
                .append(Component.text(" "))
                .append(Component.text(sender.username + ": "))
                .append(message)
        );
    }


}
