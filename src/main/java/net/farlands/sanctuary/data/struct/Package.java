package net.farlands.sanctuary.data.struct;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a package being sent to another player.
 */
public record Package(
    UUID senderUuid,
    String senderName,
    ItemStack item,
    Component message,
    long sentTime,
    boolean forceSend
) {

    public static final int expirationTime = 1000 * 60 * 60 * 24 * 7;


    public Package(UUID senderUuid, String senderName, ItemStack item, Component message, boolean forceSend) {
        this(senderUuid, senderName, item, message, System.currentTimeMillis(), forceSend);
    }

    // Forced packages do not expire or return to sender
    public boolean hasExpired() {
        return !forceSend && sentTime + expirationTime < System.currentTimeMillis();
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
