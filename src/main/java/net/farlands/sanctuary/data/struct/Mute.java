package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Objects;

/**
 * Handles player mutes.
 */
public final class Mute {

    private final long dateEnds;
    private final String reason;

    /**
     */
    public Mute(long dateEnds, String reason) {
        this.dateEnds = dateEnds;
        this.reason = reason;
    }

    public Mute(int duration, String reason) {
        this(System.currentTimeMillis() + duration * 1000L, reason);
    }

    public Mute(int duration) {
        this(duration, "Muted by a staff member.");
    }

    public Mute() {
        this(0, "");
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > dateEnds;
    }

    public long timeRemaining() {
        return dateEnds - System.currentTimeMillis();
    }

    public void sendMuteMessage(CommandSender sender) {
        sender.sendMessage(
            ChatColor.RED + "You may not type in chat. You were muted for: " + ChatColor.GOLD + reason +
            ChatColor.RED + " Your mute expires in " + TimeInterval.formatTime(timeRemaining(), false) + "."
        );
    }

    public long dateEnds() {
        return dateEnds;
    }

    public String reason() {
        return reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Mute) obj;
        return this.dateEnds == that.dateEnds &&
               Objects.equals(this.reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateEnds, reason);
    }

    @Override
    public String toString() {
        return "Mute[" +
               "dateEnds=" + dateEnds + ", " +
               "reason=" + reason + ']';
    }

}
