package net.farlands.sanctuary.mechanic;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Base class for plugin mechanics.
 */
public class Mechanic implements Listener {

    /**
     * Runs when the server starts
     */
    public void onStartup() {
    }

    /**
     * Runs when the server shuts down
     */
    public void onShutdown() {
    }

    /**
     * Runs when a player connects to the server
     *
     * @param player The player in question
     * @param isNew  If this is the player's first time
     */
    public void onPlayerJoin(Player player, boolean isNew) {
    }

    /**
     * Runs when a player disconnects from the server
     *
     * @param player The player in question
     */
    public void onPlayerQuit(Player player) {
    }
}
