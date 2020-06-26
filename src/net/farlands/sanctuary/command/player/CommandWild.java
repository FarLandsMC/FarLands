package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import static com.kicas.rp.util.Utils.*;

import static net.farlands.sanctuary.util.FLUtils.RNG;
import static net.farlands.sanctuary.util.FLUtils.tpPlayer;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.TimeInterval;
import net.farlands.sanctuary.util.FLUtils;

import net.md_5.bungee.api.ChatMessageType;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;


public class CommandWild extends PlayerCommand {
    private final Cooldown globalCooldown;

    public CommandWild() {
        super(Rank.INITIATE, Category.TELEPORTING, "Teleport to a random location on the map.", "/wild", "wild", "rtp");
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

    private static boolean isSea(Block block) {
        return block.isLiquid() || block.isPassable() || block.getType() == Material.SEA_PICKLE;
    }

    // in case we decide to make a portal so we can copy into utils
    public static void rtpPlayer(final Player player, final int range) {
        int dx = 1 + RNG.nextInt(range << 1) - range,
            zMax = 1 + (int) Math.sqrt(range * range - dx * dx),
            dz = 1 + RNG.nextInt(zMax << 1) - zMax;

        Location rtp = new Location(
                player.getWorld(),
                dx,
                62,
                dz,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        StringBuilder debugMessage = new StringBuilder();
        final String debugPre = "unsafe rtp @";
        debugMessage.append(debugPre);

        Location safe;
        for (;;) {
            if (isSea(rtp.getBlock())) {
                dx = 1 + RNG.nextInt(range << 1) - range;
                rtp.setX(dx);
            } else {
                safe = rtpFindSafe(rtp, debugMessage);
                if (safe != null)
                    break;
            }
            if (isSea(rtp.getBlock())) {
                zMax = 1 + (int) Math.sqrt(range * range - dx * dx);
                dz = 1 + RNG.nextInt(zMax << 1) - zMax;
                rtp.setZ(dz);
            } else {
                safe = rtpFindSafe(rtp, debugMessage);
                if (safe != null)
                    break;
            }
        }
        if (debugMessage.length() > debugPre.length())
            FarLands.getDebugger().echo(debugMessage.toString());
        tpPlayer(player, safe);

        if (FarLands.getDataHandler().getOfflineFLPlayer(player).homes.isEmpty()) {
           sendFormatted(player, ChatMessageType.ACTION_BAR, "&(aqua)You have no homes, use /sethome [name] " +
                    "so you can safely return to your location!");
        }
    }

    private static Location rtpFindSafe(final Location origin, StringBuilder debugMessage) {
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

        debugMessage.append("\n")
                .append(safe.getBlockX()).append(" ")
                .append(safe.getBlockY()).append(" ")
                .append(safe.getBlockZ());
        return null;
    }
}
