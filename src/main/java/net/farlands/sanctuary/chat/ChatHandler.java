package net.farlands.sanctuary.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandMessage;
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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * A central class for handling all chat features
 */
public class ChatHandler {

    /**
     * Run event on a player login or logout
     * @param event Either {@link PlayerJoinEvent} or {@link PlayerQuitEvent}
     */
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
                    "{} has {} silently.",
                    event.getPlayer().getName(),
                    join ? "joined" : "left"
                ),
                DiscordChannel.STAFF_COMMANDS
            );
        } else {
            playerTransition(event.getPlayer(), flp, join);
        }

    }

    /**
     * Send a player's transition message to chat
     * @param player Player in question (used to get name if changed)
     * @param flp Player in question
     * @param join If the player was transitioning into the game
     */
    public static void playerTransition(Player player, OfflineFLPlayer flp, boolean join) {
        String joinOrLeave = join ? "joined" : "left";
        Component message = ComponentColor.yellow(
            "{:yellow bold} {} has {}.",
            '>',
            flp,
            joinOrLeave
        );
        Bukkit.broadcast(message);
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, message);

    }

    /**
     * Method run on chat event
     */
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

        Component formatted = handleReplacements(message, sender);

        if (!shout) { // If the player isn't "shouting" (! before message)
            FLPlayerSession session = FarLands.getDataHandler().getSession(player);
            if (sender.vanished || session.autoSendStaffChat) { // Send to sc
                staffChat(player, PlainTextComponentSerializer.plainText().serialize(formatted));
                return;
            }

            if (session.replyToggleRecipient != null) { // Auto reply
                if (session.replyToggleRecipient instanceof Player && ((Player) session.replyToggleRecipient).isOnline()) {
                    CommandMessage.sendMessages(
                        FarLands.getDataHandler().getOfflineFLPlayer(session.replyToggleRecipient),
                        session.handle,
                        ComponentUtils.toText(event.message())
                    );
                    return;
                }
            }
        }
        chat(sender, formatted);

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
     * @param message Message Text
     */
    public static void chat(OfflineFLPlayer sender, Component message) {
        chat(sender, getPrefix(sender), message);
    }

    /**
     * Send chat message as OfflineFLPlayer
     *
     * @param sender  Sender of chat message
     * @param prefix  Sender's prefix
     * @param message Message Text
     */
    public static void chat(OfflineFLPlayer sender, Component prefix, Component message) {
        Component censorMessage = MessageFilter.INSTANCE.censor(message);

        sendToConsole(message, sender);
        broadcast(prefix, message, censorMessage, sender); // Send to players
        broadcastDiscord(prefix, message); // Send to #in-game
    }

    public static Component getPrefix(OfflineFLPlayer sender) {
        Component nameDisplay = ComponentUtils.suggestCommand(
            "/msg " + sender.username + " ",
            sender.getDisplayName(), // Colored Username or Nickname
            CommandStats.getFormattedStats(sender, null, false)
        );
        return ComponentColor.color(
            sender.getDisplayRank().color(),
            "{} {}:",
            sender.getDisplayRank(),
            nameDisplay
        );
    }

    public static Component handleReplacements(String message, OfflineFLPlayer sender) {

        Component component = MiniMessageWrapper.farlands(sender).mmParse(message); // Parse colors and mm tags
        component = ChatFormat.translateAll(component, sender); // Translate all chat features

        return component;
    }

    private static boolean handleCensor(Player player, OfflineFLPlayer sender, String message) {
        if (sender.rank.isStaff()) return false;
        if (MessageFilter.INSTANCE.autoCensor(message)) {
            boolean alertPlayer = sender.secondsPlayed > 60 * 15;
            if (alertPlayer) { // More than 15 minutes of playtime
                player.sendMessage(
                    ComponentColor.red(
                        "Your message has not been sent, because it may contain messages or phrases offensive to some"
                    )
                );
            } else { // Send a false message
                player.sendMessage(ComponentColor.white("{} {}", getPrefix(sender), message));
            }
            Logging.broadcastStaff(
                ComponentColor.red(
                    "[AUTO-CENSOR] {}: {:gray} - {}",
                    sender.username,
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
        Logging.broadcastStaff(ComponentColor.red("[MUTED] {}: {:gray}", flp.username, message));
        return true;
    }

    public static void broadcastDiscord(Component prefix, Component messageC) {
        String prefixStr = MarkdownProcessor.escapeMarkdown(ComponentUtils.toText(prefix));
        String message = MarkdownProcessor.fromMinecraft(messageC);
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.IN_GAME, MarkdownProcessor.removeChatColor(prefixStr + " " + message));
    }

    /**
     * Send message only to specific players
     *
     * @param message   The message to send
     * @param selection The selected players
     */
    public static void broadcast(Component message, Player... selection) {
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
            .forEach(flp -> flp.getOnlinePlayer().sendMessage(
                ComponentUtils.format("{} {}", prefix, flp.censoring ? censorMessage : message)
            ));
    }

    public static void sendToConsole(Component message, OfflineFLPlayer sender) {
        Bukkit.getConsoleSender().sendMessage(ComponentUtils.format("{} {}: {}", sender.rank, sender.username, message));
    }


}
