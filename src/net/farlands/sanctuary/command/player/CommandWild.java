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
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;


public class CommandWild extends PlayerCommand {
    private static final int INNER_RAD =  1500,
                         MIN_OUTER_RAD =  7500,
                         MAX_OUTER_RAD = 20000;

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

        if (!"world".equals(sender.getWorld().getName())) {
            if (session.handle.rank.specialCompareTo(Rank.DONOR) < 0) {
                sendFormatted(sender, "&(red)You can only use this command in the overworld.");
                return true;
            }
            if (!"world_nether".equals(sender.getWorld().getName())) {
                sendFormatted(sender, "&(red)You can only use this command in the overworld and nether.");
                return true;
            }
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

        long time = System.currentTimeMillis() - FarLands.getDataHandler().getPluginData().seasonStartTime;
        rtpPlayer(
                sender,
                INNER_RAD,
                MIN_OUTER_RAD + Math.min(MAX_OUTER_RAD - MIN_OUTER_RAD, (int) (time / 180000L)) // 3 * 60 * 1000, 3 minutes per block of rtp
        );
        return true;
    }

    private static boolean quickCheck(Block block) {
        return !(block.isLiquid() || block.isPassable() || block.getType() == Material.SEA_PICKLE);
    }

    private static int getRandom(int min, int max) {
        return RNG.nextBoolean() ?
                min + RNG.nextInt(max - min) :
               -min - RNG.nextInt(max - min);
    }

    // in case we decide to make a portal so we can copy into utils
    public static void rtpPlayer(Player player, int minRange, int maxRange) {
        boolean overworld = player.getWorld().getName().equals("world");
        int dx = getRandom(minRange, maxRange),
            dz = getRandom(
                    dx >= minRange ? 0 : (int)Math.sqrt(minRange * minRange - dx * dx),
                    (int)Math.sqrt(maxRange * maxRange - dx * dx)
            );
        Location rtp = new Location(
                player.getWorld(),
                dx,
                overworld ? 62 : 31,
                dz,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        Location safe = null;
        int death = 64;
        for (; --death >= 0;) {
            if (quickCheck(rtp.getBlock())) {
                safe = overworld ? rtpFindSafe(rtp) : findSafe(rtp, 0, 128);
                if (safe != null)
                    break;
            }
            rtp.setX(dx = getRandom(
                    dz >= minRange ? 0 : (int) Math.sqrt(minRange * minRange - dz * dz),
                    (int) Math.sqrt(maxRange * maxRange - dz * dz)
            ));

            if (quickCheck(rtp.getBlock())) {
                safe = overworld ? rtpFindSafe(rtp) : findSafe(rtp, 0, 128);
                if (safe != null)
                    break;
            }
            rtp.setZ(dz = getRandom(
                    dx >= minRange ? 0 : (int) Math.sqrt(minRange * minRange - dx * dx),
                    (int) Math.sqrt(maxRange * maxRange - dx * dx)
            ));
        }
        if (safe == null) {
            // Pretty much never happens in practice
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1, 1);
            sendFormatted(player, "&(red)Your random teleport randomly failed :(");
            return;
        }
        FarLands.getDebugger().echo(player.getName() + " /rtp -> " +
                safe.getWorld().getName() + " " +
                safe.getBlockX() + " " +
                safe.getBlockY() + " " +
                safe.getBlockZ() + " : " + (64 - death)
        );
        tpPlayer(player, safe);

        if (FarLands.getDataHandler().getOfflineFLPlayer(player).homes.isEmpty()) {
           sendFormatted(player, ChatMessageType.ACTION_BAR, "&(aqua)You have no homes, use /sethome [name] " +
                    "so you can safely return to your location!");
        }
    }

    private static Location rtpFindSafe(Location origin) {
        Location safe = origin.clone();
        safe.setX(safe.getBlockX() + .5);
        safe.setZ(safe.getBlockZ() + .5);
        safe.getChunk().load();
        int bottom = 62, top = 1 + safe.getChunk().getChunkSnapshot().getHighestBlockYAt(safe.getBlockX() & 15, safe.getBlockZ() & 15);

        if (canStand(safe.getBlock()) && isSafe(safe.clone()))
            return safe.add(0, .5, 0);

        do {
            safe.setY((bottom + top + 1) >> 1);
            if (safe.getBlock().getLightFromSky() <= 8)
                bottom = safe.getBlockY();
            else
                top = safe.getBlockY();
        } while (top - bottom > 1);
        safe.setY((bottom + top - 1) >> 1);

        if (canStand(safe.getBlock()) && isSafe(safe.clone()))
            return safe.add(0, 1.5, 0);
        return null;
    }
}
