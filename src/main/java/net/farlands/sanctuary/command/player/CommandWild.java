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
import org.bukkit.block.Biome;
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

    private static boolean biomeCheck(Biome biome) {
        return biome.toString().endsWith("FROZEN_OCEAN") || !(biome.toString().endsWith("OCEAN") || biome == Biome.RIVER);
    }

    private static void rtpFail(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1, 1);
        player.sendMessage(ComponentColor.red("Your random teleport randomly failed :("));
        FarLands.getDebugger().echo(player.getName() + " /rtp → fail");
    }

    public static void rtpPlayer(Player player, int minRange, int maxRange) throws ExecutionException, InterruptedException {
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
        outer:
        while (--ttl >= 0) {
            if (biomeCheck(rtp.getBlock().getBiome())) { // does not access the chunk
                /* things that do access the chunk:
                 * Block.getType() return CraftBlockType.minecraftToBukkit(this.world.getBlockState(this.position).getBlock());
                 * Block.isSolid() (no implementation)
                 * Block.isLiquid() return this.getNMS().liquid();
                 */ // FarLands.getDebugger().echo(System.currentTimeMillis() + "\n" + rtp.getBlock().isLiquid() + "\n" + System.currentTimeMillis());
                if ((safe = overworld ? rtpFindSafe(rtp) : findSafe(rtp, 0, 126)) != null)
                    break;

                // check chunk corners
                int cx = rtp.getBlockX() & 15;
                int cz = rtp.getBlockZ() & 15;
                x -= cx;
                z -= cz;
                // psuedo-random walk over all chunk x,z positions:  0 5 10 15 4 9 14 3 8 13 2 7 12 1 6 11
                for (int dz = 0; dz <= 75; dz += 5) {
                    rtp.setZ(z + (cz + dz) % 16);
                    for (int dx = 0; dx <= 75; dx += 5) {
                        rtp.setX(x + (cx + dx) % 16);
                        if ((safe = overworld ? rtpFindSafe(rtp) : findSafe(rtp, 0, 126)) != null)
                            break outer;
                    }
                }
            }

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
                safe.getBlockX() + " " + safe.getBlockY() + " " + safe.getBlockZ() +
                " : " + (64 - ttl)
        );
        Location finalSafe = safe;
        FarLands.getInstance().getServer().getScheduler().runTask(FarLands.getInstance(), () -> tpPlayer(player, finalSafe));
        player.sendMessage(ComponentColor.gray(
                "Your random teleport was successful: " +
                safe.getBlockX() + " " + safe.getBlockY() + " " + safe.getBlockZ()
        ));
        if (FarLands.getDataHandler().getOfflineFLPlayer(player).homes.isEmpty()) {
            player.sendActionBar(
                ComponentColor.aqua(
                    "You have no homes, use /sethome [name] so you can return safely to your location!"
                )
            );
        }
    }

    // copy-paste from RP utils, modified to avoid rtp over deep water
    private static boolean doesDamage(Block block) {
        return block.getType().isSolid() || block.isLiquid() ||
                block.getType() == Material.FIRE ||
                block.getType() == Material.CACTUS ||
                block.getType() == Material.SWEET_BERRY_BUSH ||
                block.getType() == Material.WITHER_ROSE ||
                block.getType() == Material.POWDER_SNOW;
    }
    private static boolean rtpIsSafe(Location location) {
        Location loc = location.clone().add(0, 1, 0);
        return !(
                (loc.getBlock().getType() != Material.WATER && doesDamage(loc.getBlock())) ||
                doesDamage(loc.add(0, 1, 0).getBlock())
        );
    }
    private static boolean rtpCanStand(Location location) {
        Block block = location.getBlock();
        return !(
                // avoid rtp on top of trees
                (block.getType().toString().endsWith("_LEAVES") && !block.getBiome().toString().endsWith("JUNGLE")) ||
                block.isPassable() ||
                block.getType() == Material.MAGMA_BLOCK ||
                block.getType() == Material.CACTUS ||
                block.getType() == Material.POWDER_SNOW
        ) || location.clone().add(0, 1, 0).getBlock().getType() == Material.LILY_PAD;
    }

    public static Location rtpFindSafe(Location origin) {
        Location safe = origin.clone();
        safe.setX(safe.getBlockX() + .5);
        safe.setZ(safe.getBlockZ() + .5);
        int bottom = 61,
            top = safe.getChunk().getChunkSnapshot().getHighestBlockYAt( // standing block
                    safe.getBlockX() & 15, safe.getBlockZ() & 15
            );

        safe.setY(top);
        if (rtpCanStand(safe) && rtpIsSafe(safe))
            return safe.add(0, 1.5, 0);

        ++top;
        do {
            safe.setY((bottom + top) >> 1);
            if (safe.getBlock().getLightFromSky() <= 8)
                bottom = safe.getBlockY();
            else
                top = safe.getBlockY() - 1;
        } while (top - bottom > 1);
        safe.setY((bottom + top) >> 1);

        if (rtpCanStand(safe) && rtpIsSafe(safe))
            return safe.add(0, 1.5, 0);
        return null;
    }
}
