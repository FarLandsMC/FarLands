package net.farlands.sanctuary.mechanic;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * Handle rotating chat messages
 */
public class RotatingMessages extends Mechanic {

    private static final long GAP = 5 * 60 * 20; // 5 minutes * 60 seconds * 20 ticks

    /**
     * All messages that should be broadcast
     */
    private static final List<Message> DEFAULT_MESSAGES = new ArrayList<>(List.of(
        /* Vote    */ new Message("<gold>Want to help support the server? Vote for us with <click:run_command:/vote><aqua>/vote</aqua></click>!"),
        /* Donate  */ new Message("<gold>Want to help support the server? Donate to us with <click:run_command:/donate><aqua>/donate</aqua></click>!"),
        /* Discord */ new Message(flp -> !flp.isDiscordVerified(), "<gold>Type <click:open_url:" + FarLands.getFLConfig().discordInvite + "><aqua>/discord</aqua></click> to join our Discord!")
    ));

    private final Queue<Message> queue = new LinkedList<>(DEFAULT_MESSAGES); // Queue for cycling through messages

    @Override
    public void onStartup() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(
            FarLands.getInstance(), // Plugin
            this::broadcast,
            30 * 20, // 30 seconds
            GAP
        );
    }

    /**
     * Broadcast the next message and cycle
     */
    public void broadcast() {
        Message msg = queue.poll(); // Cycle message to end
        queue.offer(msg);

        if (msg == null) return;
        msg.broadcast();
    }

    /**
     * Add a message to the cycle
     *
     * @param message The message to add -- Formatted with MiniMessage
     */
    public void addMessage(String message) {
        queue.add(new Message(message));
    }


    /**
     * Add a message to the cycle
     *
     * @param filter  Filter for which players to send the message to
     * @param message The message to add -- Formatted with MiniMessage
     */
    public void addMessage(Predicate<OfflineFLPlayer> filter, String message) {
        queue.add(new Message(filter, message));
    }

    /**
     * Data class for rotating messages.
     */
    private static record Message(Predicate<OfflineFLPlayer> filter, Component message) {

        private static final Predicate<OfflineFLPlayer> ALL = flp -> true;

        private Message(String message) {
            this(ALL, message);
        }

        private Message(Predicate<OfflineFLPlayer> filter, String message) {
            this(filter, MiniMessage.miniMessage().deserialize(message));
        }

        private void broadcast() {
            Bukkit.getOnlinePlayers()
                .stream()
                .map(FarLands.getDataHandler()::getOfflineFLPlayer)
                .filter(filter)
                .map(OfflineFLPlayer::getOnlinePlayer)
                .forEach(player -> player.sendMessage(this.message));
        }
    }
}
