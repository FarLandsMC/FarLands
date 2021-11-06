package net.farlands.sanctuary.data.struct;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a package being sent to another player.
 */
public record Package(UUID senderUuid, String senderName, ItemStack item, String message, long sentTime, boolean forceSend) {
    public static final int expirationTime = 1000 * 60 * 60 * 24 * 7;

    public Package(UUID senderUuid, String senderName, ItemStack item, String message, boolean forceSend) {
        this(senderUuid, senderName, item, message, System.currentTimeMillis(), forceSend);
    }

    // Forced packages do not expire or return to sender
    public boolean hasExpired() {
        return !forceSend && sentTime + expirationTime < System.currentTimeMillis();
    }
}
