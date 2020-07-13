package net.farlands.sanctuary.data.struct;

import com.kicas.rp.util.TextUtils;
import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.util.FLUtils;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.server.v1_16_R1.CommandAdvancement;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class TeleportRequest implements Runnable {
    private final TeleportType type;
    private final Player sender, recipient, teleporter, anchor;
    private int expirationTaskUid;
    private int delay;
    private int delayTaskUid;
    private Location startLocationTeleporter; // This is to keep track of whether the teleporter has moved
    private Location toLocation; // Where we're going

    public TeleportRequest(TeleportType type, Player sender, Player recipient) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.teleporter = TeleportType.SENDER_TO_RECIPIENT.equals(type) ? sender : recipient;
        this.anchor = TeleportType.SENDER_TO_RECIPIENT.equals(type) ? recipient : sender;
        this.delay = FarLands.getDataHandler().getOfflineFLPlayer(teleporter).rank.getTpDelay() * 20;
    }

    public boolean open() {
        // Make sure no duplicate requests are sent
        FLPlayerSession recipientSession = FarLands.getDataHandler().getSession(recipient);
        if (recipientSession.teleportRequests.stream().anyMatch(request -> request.sender.equals(sender))) {
            sender.sendMessage(ChatColor.RED + "You already have a pending teleport request with this player.");
            return false;
        }

        // Register the task with the session and setup expiration
        expirationTaskUid = FarLands.getScheduler().scheduleSyncDelayedTask(() -> {
            if (sender.isOnline())
                decline();
            else
                removeData();
        }, 2L * 60L * 20L);

        // Send messages
        boolean isSenderToRecipient = TeleportType.SENDER_TO_RECIPIENT.equals(type);
        TextUtils.sendFormatted(recipient, "&(gold){&(aqua)%0} has requested %1 Type $(command,/tpaccept,{&(aqua)/tpaccept}) " +
                        "to accept the request, or $(command,/tpdecline,{&(aqua)/tpdecline}) to decline it, or click on the commands to run them.%2",
                sender.getName(), isSenderToRecipient ? "to teleport to you." : "you to teleport to them.",
                isSenderToRecipient ? "" : " Move to cancel the teleport.");
        recipient.playSound(recipient.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
        sender.sendMessage(ChatColor.GOLD + "Request sent." + (isSenderToRecipient ? " Move to cancel the teleport." : ""));

        return true;
    }

    @Override
    public void run() {
        // If the teleporter logs off, no point in continuing
        if (!teleporter.isOnline()) {
            endTask();
            return;
        }

        // Decrement the delay
        if (delay > 0) {
            // If the teleporter moves too much,
            if (startLocationTeleporter.getWorld().equals(teleporter.getWorld()) &&
                    startLocationTeleporter.distance(teleporter.getLocation()) > 1.5D) {
                endTask();

                // Send messages
                teleporter.sendMessage(ChatColor.RED + "Teleport canceled.");
                teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 3.0F, 1.0F);
                return;
            }

            // Send a notification each second
            if (delay % 20 == 0)
                teleporter.sendMessage(ChatColor.GOLD + "Teleporting in " + (delay / 20) + " second" + (delay > 20 ? "s..." : "..."));

            --delay;
            return;
        }

        // Execute the teleport and wrap up
        Location safe = Utils.findSafe(toLocation, 0, 256);
        if (safe == null)
            teleporter.sendMessage(ChatColor.RED + "The location you were teleporting to is no longer safe.");
        else
            FLUtils.tpPlayer(teleporter, safe);
        endTask();
    }

    public void accept() { // Called by recipient
        removeData();

        // Make sure the sender didn't log off
        if (!sender.isOnline()) {
            recipient.sendMessage(ChatColor.RED + "This player is no longer online. Teleport canceled.");
            return;
        }

        // Keep track of locations for movement cancellation and the end teleportation
        startLocationTeleporter = teleporter.getLocation().clone();
        toLocation = Utils.findSafe(anchor.getLocation().clone(), 0, 256);

        // Check location safety
        if (toLocation == null) {
            anchor.sendMessage(ChatColor.RED + "Teleport canceled. Please move to a safe location and try again.");
            teleporter.sendMessage(ChatColor.RED + "Teleport canceled. Could not find a safe location to teleport to.");
            return;
        }

        // Setup the teleport delay
        delayTaskUid = FarLands.getScheduler().scheduleSyncRepeatingTask(this, 20L, 1L);

        // Send messages
        boolean isSenderToRecipient = TeleportType.SENDER_TO_RECIPIENT.equals(type);
        recipient.sendMessage(ChatColor.GOLD + "Request accepted." + (isSenderToRecipient ? " Teleporting..." : ""));
        sender.sendMessage(ChatColor.AQUA + recipient.getName() + ChatColor.GOLD + " has accepted your teleport request." +
                (isSenderToRecipient ? "" : " Teleporting..."));
        sender.playSound(sender.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
    }

    public void decline() { // Called by recipient
        removeData();

        // Send messages
        recipient.sendMessage(ChatColor.GOLD + "Request declined.");
        if (sender.isOnline())
            sender.sendMessage(ChatColor.AQUA + recipient.getName() + ChatColor.RED + " did not accept your teleport request.");
    }

    public Player getSender() {
        return sender;
    }

    private void removeData() {
        FarLands.getDataHandler().getSession(recipient).teleportRequests.remove(this);
        FarLands.getScheduler().cancelTask(expirationTaskUid);
    }

    private void endTask() {
        delay = Integer.MAX_VALUE;
        FarLands.getScheduler().cancelTask(delayTaskUid);
    }

    public enum TeleportType {
        SENDER_TO_RECIPIENT, RECIPIENT_TO_SENDER
    }
}
