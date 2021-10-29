package net.farlands.sanctuary.command;

import net.farlands.sanctuary.FarLands;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event for plugin shutdown.
 */
public class FLShutdownEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public FLShutdownEvent() {
        FarLands.getScheduler().scheduleSyncDelayedTask(Bukkit::shutdown, 5L);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}