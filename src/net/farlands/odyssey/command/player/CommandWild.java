package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Cooldown;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;

import net.farlands.odyssey.util.FLUtils;
import net.md_5.bungee.api.ChatMessageType;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static net.farlands.odyssey.util.FLUtils.RNG;
import static net.farlands.odyssey.util.FLUtils.tpPlayer;
import static com.kicas.rp.util.Utils.*;

public class CommandWild extends PlayerCommand {
    private final Cooldown globalCooldown;

    public CommandWild() {
        super(Rank.INITIATE, "Teleport to a random location on the map.", "/wild", "wild", "rtp");
        this.globalCooldown = new Cooldown(200L);
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        long timeRemaining = session.commandCooldownTimeRemaining(this) * 50L;
        if (timeRemaining > 0L) {
            sendFormatted(sender, "&(red)You can use the command again in " + TimeInterval.formatTime(timeRemaining, false) + ".");
            return true;
        }

        if (!"world".equals(sender.getWorld().getName())) { // rtpFindSafe is optimized for overworld and cannot be used if this changes
            sendFormatted(sender, "&(red)You can only use this command in the overworld.");
            return true;
        }

        if (FLUtils.serverMspt() > 80) {
            sendFormatted(sender, "&(red)The server is too laggy right now to use this command.");
            return true;
        }

        if (!globalCooldown.isComplete()) {
            sendFormatted(sender, "&(red)You cannot use this command right now. Try again in a few seconds.");
            return true;
        }

        globalCooldown.reset();

        int wildCooldown = Rank.getRank(sender).getWildCooldown();
        if (wildCooldown > 0)
            session.setCommandCooldown(this, wildCooldown * 60L * 20L);
        rtpPlayer(sender, 15000);
        return true;
    }

    // in case we decide to make a portal so we can copy into utils
    public static void rtpPlayer(final Player player, final int range) {
        Location rtp = new Location(player.getWorld(),
                RNG.nextInt(2 * range) - range, 62, RNG.nextInt(2 * range) - range,
                player.getLocation().getYaw(), player.getLocation().getPitch());
        while (rtp.getBlock().isLiquid() || rtp.getBlock().isPassable() || rtp.getBlock().getType().equals(Material.SEA_PICKLE)) {
            rtp.setX(RNG.nextInt(2 * range) - range);
            if (rtp.getBlock().isLiquid())
                rtp.setZ(RNG.nextInt(2 * range) - range);
        }
        final int x = rtp.getX() > 0 ? -4 : 4,
                z = rtp.getZ() > 0 ? -4 : 4;
        Location temp = rtpFindSafe(rtp);
        while (temp == null) {
            rtp.setX(rtp.getBlockX() + x);
            rtp.setZ(rtp.getBlockZ() + z);
            temp = rtpFindSafe(rtp);
        }
        tpPlayer(player, temp);
        if (FarLands.getDataHandler().getOfflineFLPlayer(player).homes.isEmpty()) {
           sendFormatted(player, ChatMessageType.ACTION_BAR, "&(aqua)You have no homes, use /sethome [name] " +
                    "so you can safely return to your location!");
        }
    }

    private static Location rtpFindSafe(final Location origin) {
        Location safe = origin.clone();
        safe.setX(safe.getBlockX() + .5);
        safe.setZ(safe.getBlockZ() + .5);
        safe.getChunk().load();
        if (canStand(safe.getBlock()) && isSafe(safe.clone()))
            return safe.add(0, .5, 0);
        int s = 62, e = 254;
        do {
            safe.setY((s + e + 1) >> 1);
            if (safe.getBlock().getLightFromSky() <= 8)
                s = safe.getBlockY();
            else
                e = safe.getBlockY();
        } while (e - s > 1);
        safe.setY((s + e - 1) >> 1);
        if (canStand(safe.getBlock()) && isSafe(safe.clone()))
            return safe.add(0, 1.5, 0);
        FarLands.getDebugger().echo("unsafe rtp @ " +
                safe.getBlockX() + " " + safe.getBlockY() + " " + safe.getBlockZ());
        return null;
    }
}
