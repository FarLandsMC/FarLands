package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.RandomAccessDataHandler;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class TeleportRequest implements Runnable {
    private final boolean type; // false: Sender -> Recipient; true: Recipient -> Sender
    private final Player sender;
    private final Player recipient;
    private final Player teleporter; // Alias for the person actually teleporting
    private final Player anchor;
    private final String cooldownUid;
    private final RandomAccessDataHandler radh; // Alias to the random access data handler
    private int delay;
    private int taskUid;
    private Location startLocationTeleporter; // This is to keep track of whether the teleporter has moved
    private Location toLocation; // Where we're going

    public static final String COOLDOWN_CATEGORY = "tpa";
    public static final String REQUEST_CATEGORY = "tpareq";

    private TeleportRequest(boolean type, Player sender, Player recipient) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.teleporter = type ? recipient : sender;
        this.anchor = type ? sender : recipient;
        this.cooldownUid = Utils.combineUUIDs(sender.getUniqueId(), recipient.getUniqueId()).toString();
        this.radh = FarLands.getDataHandler().getRADH();
        this.delay = FarLands.getPDH().getFLPlayer(teleporter).getRank().getTpDelay() * 20;
    }

    @SuppressWarnings("unchecked")
    public static void newRequest(boolean type, Player sender, Player recipient) {
        final TeleportRequest req = new TeleportRequest(type, sender, recipient);
        if(!req.radh.isCooldownComplete(COOLDOWN_CATEGORY, req.cooldownUid)) {
            sender.sendMessage(ChatColor.RED + "You already have a pending teleport request with this player.");
            return;
        }

        ((List<TeleportRequest>)req.radh.retrieveAndStoreIfAbsent(
            new ArrayList<TeleportRequest>(), REQUEST_CATEGORY, recipient.getUniqueId().toString()
        )).add(req); // Add this request to the recipient's request list
        TextUtils.sendFormatted(recipient, "&(gold){&(aqua)%0} has requested %1 Type $(command,/tpaccept,{&(aqua)/tpaccept}) " +
                "to accept the request, or $(command,/tpdecline,{&(aqua)/tpdecline}) to decline it, or click on the commands to run them.%2",
                sender.getName(), type ? "you to teleport to them." : "to teleport to you.", type ? " Move to cancel the teleport." : "");
        recipient.playSound(recipient.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);

        req.radh.setCooldown(2L * 60L * 20L, COOLDOWN_CATEGORY, req.cooldownUid, () -> {
            if(sender.isOnline())
                req.decline();
            else
                req.removeData();
        }); // After five minutes the request expires
        sender.sendMessage(ChatColor.GOLD + "Request sent." + (type ? "" : " Move to cancel the teleport."));
    }

    @Override
    public void run() {
        if(!teleporter.isOnline()) {
            endTask();
            return;
        }
        if(delay > 0) {
            if(startLocationTeleporter.getWorld().equals(teleporter.getWorld()) &&
                    startLocationTeleporter.distance(teleporter.getLocation()) > 1.5D) { // If the teleporter moves, cancel
                teleporter.sendMessage(ChatColor.RED + "Teleport canceled.");
                teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 3.0F, 1.0F);
                endTask();
                return;
            }
            if(delay % 20 == 0)
                teleporter.sendMessage(ChatColor.GOLD + "Teleporting in " + (delay / 20) + " second" + (delay > 20 ? "s..." : "..."));
            -- delay;
            return;
        }
        Utils.tpPlayer(teleporter, toLocation);
        endTask();
    }

    public void accept() { // Called by recipient
        if(!sender.isOnline()) {
            recipient.sendMessage(ChatColor.RED + "This player is no longer online. Teleport canceled.");
            return;
        }
        startLocationTeleporter = teleporter.getLocation();
        toLocation = Utils.findSafe(anchor.getLocation());
        if(toLocation == null) {
            anchor.sendMessage(ChatColor.RED + "Teleport canceled. Please move to a safe location and try again.");
            teleporter.sendMessage(ChatColor.RED + "Teleport canceled. Could not find a safe location to teleport to.");
            removeData();
            return;
        }
        recipient.sendMessage(ChatColor.GOLD + "Request accepted." + (type ? " Teleporting..." : ""));
        sender.sendMessage(ChatColor.AQUA + recipient.getName() + ChatColor.GOLD + " has accepted your teleport request." + (type ? "" : " Teleporting..."));
        sender.playSound(sender.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
        removeData();
        taskUid = FarLands.getScheduler().scheduleSyncRepeatingTask(this, 0L, 1L);
    }

    public void decline() { // Called by recipient
        sender.sendMessage(ChatColor.GOLD + "Request declined.");
        if(sender.isOnline())
            sender.sendMessage(ChatColor.AQUA + recipient.getName() + ChatColor.RED + " did not accept your teleport request.");
        removeData();
    }

    public Player getSender() {
        return sender;
    }

    @SuppressWarnings("unchecked")
    private void removeData() {
        radh.removeCooldown(COOLDOWN_CATEGORY, cooldownUid);
        List<TeleportRequest> tpreqList = (List<TeleportRequest>)radh.retrieve(REQUEST_CATEGORY, recipient.getUniqueId().toString());
        tpreqList.remove(this);
        if(tpreqList.isEmpty())
            radh.delete(REQUEST_CATEGORY, recipient.getUniqueId().toString());
    }

    private void endTask() {
        delay = Integer.MAX_VALUE;
        FarLands.getScheduler().cancelTask(taskUid);
    }
}
