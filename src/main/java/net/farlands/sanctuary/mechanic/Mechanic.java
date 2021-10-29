package net.farlands.sanctuary.mechanic;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Base class for plugin mechanics.
 */
public class Mechanic implements Listener {
    public void onStartup() { }

    public void onShutdown() { }

    public void onPlayerJoin(Player player, boolean isNew) { }

    public void onPlayerQuit(Player player) { }
}
