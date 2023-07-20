package net.farlands.sanctuary.mechanic;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
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
        new Message( // Vote
            flp -> flp.votesToday < FarLands.getFLConfig().voteConfig.voteLinks.size(),
            ComponentColor.gold("Want to help support the server? Vote for us with {}!", ComponentUtils.command("/vote"))
        ),
        new Message( // Donate
            ComponentColor.gold("Want to help support the server? Consider donating at {}!", ComponentUtils.command("/donate"))
        ),
        new Message( // Discord
            flp -> !flp.isDiscordVerified(),
            ComponentColor.gold("Type {} to join our Discord!", ComponentUtils.command("/discord"))
        )
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
        Message msg = this.queue.poll(); // Cycle message to end
        this.queue.offer(msg);

        if (msg == null) return;
        msg.broadcast();
    }

    /**
     * Add a message to the cycle
     *
     * @param message The message to add -- Formatted with MiniMessage
     */
    public void addMessage(String message) {
        this.queue.add(new Message(message));
    }


    /**
     * Add a message to the cycle
     *
     * @param filter  Filter for which players to send the message to
     * @param message The message to add -- Formatted with MiniMessage
     */
    public void addMessage(Predicate<OfflineFLPlayer> filter, String message) {
        this.queue.add(new Message(filter, message));
    }

    /**
     * Data class for rotating messages.
     */
    private record Message(Predicate<OfflineFLPlayer> filter, Component message) {

        private static final Predicate<OfflineFLPlayer> ALL = flp -> true;

        private Message(String message) {
            this(ALL, message);
        }

        private Message(Component message) {
            this(ALL, message);
        }

        private Message(Predicate<OfflineFLPlayer> filter, String message) {
            this(filter, MiniMessage.miniMessage().deserialize(message));
        }

        private void broadcast() {
            Bukkit.getOnlinePlayers()
                .stream()
                .map(FarLands.getDataHandler()::getOfflineFLPlayer)
                .filter(this.filter)
                .map(OfflineFLPlayer::getOnlinePlayer)
                .forEach(player -> player.sendMessage(this.message));
        }
    }
}
