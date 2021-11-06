package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Handles player mutes.
 */
public record Mute(long dateEnds, String reason) {
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
}
