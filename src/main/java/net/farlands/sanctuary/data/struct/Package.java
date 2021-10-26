package net.farlands.sanctuary.data.struct;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Package {
    public static final int expirationTime = 1000 * 60 * 60 * 24 * 7;

    public final UUID      senderUuid;
    public final String    senderName;
    public final ItemStack item;
    public final String    message;
    public final long      sentTime;
    public final boolean   forceSend;

    public Package(UUID senderUuid, String senderName, ItemStack item, String message, long sentTime, boolean forceSend) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.item =       item;
        this.message =    message;
        this.sentTime =   sentTime;
        this.forceSend =  forceSend;
    }
    public Package(UUID senderUuid, String senderName, ItemStack item, String message, boolean forceSend) {
        this(senderUuid, senderName, item, message, System.currentTimeMillis(), forceSend);
    }

    // Forced packages do not expire or return to sender
    public boolean hasExpired() {
        return !forceSend && sentTime + expirationTime < System.currentTimeMillis();
    }
}
