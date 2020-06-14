package net.farlands.sanctuary.command;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FLCommandEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Class<? extends Command> command;
    private final CommandSender sender;
    private boolean cancelled;

    public FLCommandEvent(Command command, CommandSender sender) {
        this.command = command.getClass();
        this.sender = sender;
        this.cancelled = false;
    }

    public Class<? extends Command> getCommand() {
        return command;
    }

    public CommandSender getSender() {
        return sender;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public void setCancelled(boolean value) {
        cancelled = value;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
