package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.concurrent.ExecutionException;

import static com.kicas.rp.util.Utils.*;
import static net.farlands.sanctuary.util.FLUtils.RNG;
import static net.farlands.sanctuary.util.FLUtils.tpPlayer;


public class CommandWild extends PlayerCommand {
    private static final int INNER_RAD =  1500,
                         MIN_OUTER_RAD =  7500,
                         MAX_OUTER_RAD = 20000;

    private final Cooldown globalCooldown;

    public CommandWild() {
        super(Rank.INITIATE, Category.TELEPORTING, "Teleport to a random location on the map.", "/wild|rtp", "wild", "rtp");
        this.globalCooldown = new Cooldown(200L);
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        long timeRemaining = session.commandCooldownTimeRemaining(this) * 50L;
        if (timeRemaining > 0L) {
            sender.sendMessage(ComponentColor.red("You can use the command again in " + TimeInterval.formatTime(timeRemaining, false) + "."));
            return true;
        }

        if (!"world".equals(sender.getWorld().getName())) {
            if (session.handle.rank.specialCompareTo(Rank.DONOR) < 0) {
                sender.sendMessage(ComponentColor.red("You can only use this command in the overworld."));
                return true;
            }
            if (!"world_nether".equals(sender.getWorld().getName())) {
                sender.sendMessage(ComponentColor.red("You can only use this command in the overworld and nether."));
                return true;
            }
        }

        if (FLUtils.serverMspt() > 80) {
            sender.sendMessage(ComponentColor.red("The server is too laggy right now to use this command."));
            return true;
        }

        if (!globalCooldown.isComplete()) {
            sender.sendMessage(ComponentColor.red("You cannot use this command right now. Try again in a few seconds."));
            return true;
        }

        globalCooldown.reset();

        int wildCooldown = Rank.getRank(sender).getWildCooldown();
        if (wildCooldown > 0)
            session.setCommandCooldown(this, wildCooldown * 60L * 20L);
        session.unsit();

        sender.sendActionBar(ComponentColor.aqua("Finding a safe place to teleport..."));

        long time = System.currentTimeMillis() - FarLands.getDataHandler().getPluginData().seasonStartTime;
        FarLands.getInstance().getServer().getScheduler().runTaskAsynchronously(FarLands.getInstance(), () -> {
            try {
                rtpPlayer(
                    sender,
                    INNER_RAD,
                    MIN_OUTER_RAD + Math.min(MAX_OUTER_RAD - MIN_OUTER_RAD, (int) (time / 180000L)) // 3 * 60 * 1000, 3 minutes per block of rtp
                );
            } catch (ExecutionException | InterruptedException e) {
                rtpFail(sender);
                e.printStackTrace();
            }
        });
        return true;
    }

    private static boolean quickCheck(Block block) {
        return !(block.isLiquid() || block.isPassable() || block.getType() == Material.SEA_PICKLE);
    }

    private static void rtpFail(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1, 1);
        player.sendMessage(ComponentColor.red("Your random teleport randomly failed :("));
        FarLands.getDebugger().echo(player.getName() + " /rtp → fail");
    }

    private static void rtpPlayer(Player player, int minRange, int maxRange) throws ExecutionException, InterruptedException {
        boolean overworld = player.getWorld().getName().equals("world");
        final int doubleMax = maxRange << 1,
                  minSQ = minRange * minRange,
                  maxSQ = maxRange * maxRange;
        int x, z, xz;
        do {
            x = RNG.nextInt(doubleMax) - maxRange;
            z = RNG.nextInt(doubleMax) - maxRange;
            xz = x*x + z*z;
        } while (minSQ > xz || xz > maxSQ);
        Location rtp = new Location(
                player.getWorld(),
                x,
                overworld ? 62 : 31,
                z,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        Location safe = null;
        int ttl = 64;
        while (--ttl >= 0) {
            if (quickCheck(rtp.getBlock()) &&
                    (safe = overworld
                            ? rtpFindSafe(rtp)
                            : findSafe(rtp, 0, 126)
                    ) != null
            )
                break;
            do {
                x = RNG.nextInt(doubleMax) - maxRange;
                z = RNG.nextInt(doubleMax) - maxRange;
                xz = x*x + z*z;
            } while (minSQ > xz || xz > maxSQ);
            rtp.setX(x);
            rtp.setZ(z);
        }
        if (safe == null) {
            // Pretty much never happens in practice
            rtpFail(player);
            return;
        }
        FarLands.getDebugger().echo(player.getName() + " /rtp → " +
                safe.getWorld().getName() + " " +
                safe.getBlockX() + " " +
                safe.getBlockY() + " " +
                safe.getBlockZ() + " : " + (64 - ttl)
        );
        Location finalSafe = safe;
        FarLands.getInstance().getServer().getScheduler().runTask(FarLands.getInstance(), () -> tpPlayer(player, finalSafe));

        if (FarLands.getDataHandler().getOfflineFLPlayer(player).homes.isEmpty()) {
            player.sendActionBar(
                ComponentColor.aqua(
                    "You have no homes, use /sethome [name] so you can return safely to your location!"
                )
            );
        }
    }

    private static Location rtpFindSafe(Location origin) throws ExecutionException, InterruptedException {
        Location safe = origin.clone();
        safe.setX(safe.getBlockX() + .5);
        safe.setZ(safe.getBlockZ() + .5);
        return safe.getWorld().getChunkAtAsync(safe, true).thenApplyAsync((chunk) -> {
            int bottom = 62,
                top = 1 + safe.getChunk().getChunkSnapshot().getHighestBlockYAt(
                        safe.getBlockX() & 15, safe.getBlockZ() & 15
                );

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
        }).get();
    }
}
