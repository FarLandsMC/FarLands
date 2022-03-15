package net.farlands.sanctuary.mechanic.anticheat;

import com.kicas.rp.util.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.checkerframework.checker.index.qual.Positive;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Mechanic to handle general anti-cheat measures.
 */
public class AntiCheat extends Mechanic {

    private final Map<UUID, XRayStore>   xray; // XRayStores for players
    private final Map<UUID, FlightStore> flight; // FlightStores for players

    private static String  lastAlertText    = null; // Text content of the last alert sent to #alerts -- for stacking of messages
    private static Message lastAlertMessage = null; // Discord Message of the last alert set to #alerts
    private static int     lastAlertCount   = 0; // Amount of alerts that have been sent in a row

    private static final long ALERT_RESET_DELAY = 5 * 60L; // Max seconds between alerts to stack messages
    private static final int  MAX_ALERT_STACK   = 5; // Max amount that alerts can stack to before sending a new message

    public AntiCheat() {
        this.xray = new HashMap<>();
        this.flight = new HashMap<>();
    }

    /**
     * Mute the flight detector for a player for a specified amount of ticks.
     */
    public void muteFlightDetector(Player player, @Positive long ticks) {
        if (this.flight.containsKey(player.getUniqueId())) {
            this.flight.get(player.getUniqueId()).mute(ticks);
        }
    }

    /**
     * Put a player into the flight and xray maps
     */
    public void put(Player player) {
        final boolean sendAlerts = !Rank.getRank(player).isStaff();
        this.xray.put(player.getUniqueId(), new XRayStore(player.getName(), sendAlerts));
        this.flight.put(player.getUniqueId(), new FlightStore(player, sendAlerts));
    }

    /**
     * Remove a player from the flight and xray maps
     */
    public void remove(Player player) {
        if (this.xray.containsKey(player.getUniqueId())) {
            this.xray.get(player.getUniqueId()).printObtained();
        }
        this.xray.remove(player.getUniqueId());
        this.flight.remove(player.getUniqueId());
    }

    /**
     * Get the X-Ray nodes for a player
     */
    public List<Pair<Detecting, Location>> getXRayNodes(UUID playerUUID) {
        if (this.xray.containsKey(playerUUID)) {
            return this.xray.get(playerUUID).getNodes();
        }
        return null;
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        if (Rank.getRank(player).specialCompareTo(Rank.MEDIA) >= 0 && !FarLands.getDataHandler().getOfflineFLPlayer(player).debugging) {
            return;
        }
        put(player);
    }

    @Override
    public void onPlayerQuit(Player player) {
        remove(player);
    }

    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.ENDER_DRAGON && event.getEntityType() == EntityType.PLAYER) {
            muteFlightDetector((Player) event.getEntity(), 10);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameMode.SURVIVAL.equals(event.getPlayer().getGameMode()) && this.xray.containsKey(event.getPlayer().getUniqueId())) {
            this.xray.get(event.getPlayer().getUniqueId()).onBlockBreak(event);
        }
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (!event.getChannel().equalsIgnoreCase("WDL|INIT")) {
            return;
        }
        broadcast(event.getPlayer().getName() + " was kicked for using World Downloader.", true);
        event.getPlayer().kick(ComponentColor.red("Please disable World Downloader."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (GameMode.SURVIVAL.equals(event.getPlayer().getGameMode()) && this.flight.containsKey(event.getPlayer().getUniqueId())) {
            this.flight.get(event.getPlayer().getUniqueId()).onUpdate();
        }
    }

    /**
     * Broadcast an anticheat message
     * @param message The message to send
     * @param sendToDiscord If the message should be sent to Discord
     */
    public static void broadcast(String message, boolean sendToDiscord) {
        Logging.broadcastStaff(ComponentColor.red("[AC] " + message), null);
        if (sendToDiscord) {
            sendDiscordAlert(message);
        }
    }

    /**
     * Send an anticheat message with the provided player's name to ingame staff and Discord#
     */
    public static void broadcast(String playerName, String message) {
        broadcast(playerName + " " + message, true);
        promptToSpec(playerName);
    }

    /**
     * Send an alert to #alerts on Discord -- handles alert stacking
     * @param alertText The text content for the alert
     */
    public static void sendDiscordAlert(String alertText) {
        if (alertText.equalsIgnoreCase(lastAlertText)) {
            long messageDelay = lastAlertMessage.isEdited() ? // Get the time period between the last update to the alert message
                OffsetDateTime.now().toEpochSecond() - Objects.requireNonNull(lastAlertMessage.getTimeEdited()).toEpochSecond() :
                OffsetDateTime.now().toEpochSecond() - lastAlertMessage.getTimeCreated().toEpochSecond();
            if (messageDelay < ALERT_RESET_DELAY && lastAlertCount < MAX_ALERT_STACK) {
                lastAlertMessage.editMessage(
                    MarkdownProcessor.escapeMarkdown(alertText) +
                    " (x" + (++lastAlertCount) + ")"
                ).queue();
                return;
            }
        }
        lastAlertText = alertText;
        lastAlertMessage = FarLands.getDiscordHandler().getChannel(DiscordChannel.ALERTS)
            .sendMessage(MarkdownProcessor.escapeMarkdown(alertText))
            .complete();
        lastAlertCount = 1;
    }

    /**
     * Prompt staff members to spectate the player with the `/spec` command
     */
    public static void promptToSpec(String playerName) {
        Logging.broadcastStaff(
            ComponentUtils.command("/spec " + playerName, ComponentColor.aqua("Spectate " + playerName))
        );
    }
}
