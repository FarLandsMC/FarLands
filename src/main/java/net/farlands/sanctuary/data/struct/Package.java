package net.farlands.sanctuary.data.struct;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a package being sent to another player.
 */
public final class Package {

    public static final int expirationTime = 1000 * 60 * 60 * 24 * 7;
    private final UUID senderUuid;
    private final String senderName;
    private final ItemStack item;
    private final String message;
    private final long sentTime;
    private final boolean forceSend;

    /**
     */
    public Package(UUID senderUuid, String senderName, ItemStack item, String message, long sentTime, boolean forceSend) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.item = item;
        this.message = message;
        this.sentTime = sentTime;
        this.forceSend = forceSend;
    }

    public Package(UUID senderUuid, String senderName, ItemStack item, String message, boolean forceSend) {
        this(senderUuid, senderName, item, message, System.currentTimeMillis(), forceSend);
    }

    // Forced packages do not expire or return to sender
    public boolean hasExpired() {
        return !forceSend && sentTime + expirationTime < System.currentTimeMillis();
    }

    public UUID senderUuid() {
        return senderUuid;
    }

    public String senderName() {
        return senderName;
    }

    public ItemStack item() {
        return item;
    }

    public String message() {
        return message;
    }

    public long sentTime() {
        return sentTime;
    }

    public boolean forceSend() {
        return forceSend;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Package) obj;
        return Objects.equals(this.senderUuid, that.senderUuid) &&
               Objects.equals(this.senderName, that.senderName) &&
               Objects.equals(this.item, that.item) &&
               Objects.equals(this.message, that.message) &&
               this.sentTime == that.sentTime &&
               this.forceSend == that.forceSend;
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderUuid, senderName, item, message, sentTime, forceSend);
    }

    @Override
    public String toString() {
        return "Package[" +
               "senderUuid=" + senderUuid + ", " +
               "senderName=" + senderName + ", " +
               "item=" + item + ", " +
               "message=" + message + ", " +
               "sentTime=" + sentTime + ", " +
               "forceSend=" + forceSend + ']';
    }

}
