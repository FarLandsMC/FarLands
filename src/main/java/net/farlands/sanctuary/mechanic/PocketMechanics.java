package net.farlands.sanctuary.mechanic;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.kicas.rp.util.Utils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.*;
import net.kyori.adventure.text.Component;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;

/**
 * Mechanics for the pocket world
 */
public class PocketMechanics extends Mechanic {

    // If this is set to `true`, then `/pocket` does not work
    public static boolean locked = false;

    public static void resetPocket(@NotNull CommandSender sender) {
        // Prevent it from unlocking if locked for some other reason
        boolean prevLocked = locked;
        locked = true;

        int playersWithHomes = 0;
        int removedHomes = 0;
        int movedPlayers = 0;

        for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {
            List<Home> partyHomes = flp.homes // Get all homes in party world
                .stream()
                .filter(home -> Worlds.POCKET.matches(home.getLocation().getWorld()))
                .toList();

            partyHomes.forEach(home -> flp.removeHome(home.getName())); // Remove the homes

            if (partyHomes.size() > 0) ++playersWithHomes; // Add to the amount of players with changed homes
            removedHomes += partyHomes.size(); // Add to the amount of removed homes

            // Teleport them to spawn if needed
            if (flp.lastLocation != null && Worlds.POCKET.matches(flp.lastLocation.asLocation().getWorld())) {
                flp.moveToSpawn();
                ++movedPlayers;
            }
        }

        FarLands.getDataHandler().getPluginData().pocketSpawn = null;

        Command.success(sender, "Deleted Pocket World spawn.");

        Command.info(
            sender,
            "Removed {} {} from {} {} and moved {} {} to server spawn.",
            removedHomes, removedHomes == 1 ? "home" : "homes",
            playersWithHomes, playersWithHomes == 1 ? "player" : "players",
            movedPlayers, movedPlayers == 1 ? "player" : "players"

        );
        Command.success(sender, "Attempting to delete world files...");
        long start = System.currentTimeMillis();
        byte[] uidDat; // uid.dat file -- saved to keep the UID of the world for like deaths and such
        File worldFolder = Worlds.POCKET.getWorld().getWorldFolder();
        try {
            Bukkit.unloadWorld(Worlds.POCKET.getWorld(), false);

            // Read uid.dat file
            uidDat = Files.readAllBytes(Paths.get(worldFolder.getAbsolutePath(), "uid.dat"));

            FileUtils.deleteDirectory(worldFolder);
            Command.success(sender, "Deleted pocket world files in {}.", TimeInterval.formatTime(System.currentTimeMillis() - start, true));
        } catch (Exception e) {
            Command.error(sender, "Unable to delete the pocket world folder");
            Logging.error("Unable to delete the pocket world folder");
            e.printStackTrace();
            return;
        }

        Command.success(sender, "Loading new pocket world, expect a lot of lag...");
        start = System.currentTimeMillis();
        try {
            // Restore uid.dat file
            worldFolder.mkdirs();
            FileOutputStream fos = new FileOutputStream(Paths.get(worldFolder.getAbsolutePath(), "uid.dat").toFile());
            fos.write(uidDat);
            fos.close();

            Worlds.POCKET.createWorld();
            World world = Worlds.POCKET.getWorld();

            WorldBorder wb = world.getWorldBorder();
            wb.setCenter(0.5, 0.5);
            wb.setSize(5000 * 2 + 1); // Diameter + 1 for centre block

            world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 1);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        } catch (Exception e) {
            Command.error(sender, "Unable to load the new pocket world");
            Logging.error("Unable to load the new pocket world");
            e.printStackTrace();
            return;
        }

        // Get a safe location near 0, 0
        Location newSpawn = Utils.findSafeNear(
            new Location(Worlds.POCKET.getWorld(), 0, 100, 0),
            50,
            200
        );
        newSpawn.add(0, 1, 0);
        FarLands.getDataHandler().getPluginData().setPocketSpawn(newSpawn);

        // Let the sender know the new spawn location
        Command.success(sender, "Set the pocket spawn to be {}", FLUtils.toSimpleString(newSpawn));

        // Feedback
        sender.sendMessage(
            ComponentColor.green(
                "Loaded new pocket world in {}",
                TimeInterval.formatTime(System.currentTimeMillis() - start, true)
            ).append(
                sender instanceof Player
                    ? Component.newline()
                    .append(ComponentUtils.command(
                        "/chain {gm3} {pocket}",
                        ComponentColor.aqua("Click here to teleport")
                    ))
                    : Component.empty()
            )
        );

        spawnPlatform(newSpawn);

        FarLands.getDiscordHandler().sendMessage(DiscordChannel.NOTEBOOK, sender.getName() + " reset the pocket world.");

        // reset the lock
        locked = prevLocked;
    }

    /**
     * Create the spawn platform for the party world
     *
     * @param spawn The location for the platform to be centred around
     */
    private static void spawnPlatform(Location spawn) {
        // Load `_pocket-world-spawn` schematic
        File file = Paths.get(
            Bukkit.getServer().getPluginsFolder().getPath(),
            "WorldEdit",
            "schematics",
            "_pocket-world-spawn.schem"
        ).toFile();

        // If it exists, then paste it
        if (file.exists()) {
            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(Worlds.POCKET.getWorld()))) {

                ClipboardFormat format = ClipboardFormats.findByFile(file);
                Clipboard clip;
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    clip = reader.read();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Operation op = new ClipboardHolder(clip)
                    .createPaste(session)
                    .to(BlockVector3.at(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()))
                    .ignoreAirBlocks(false)
                    .build();
                try {
                    Operations.complete(op);
                } catch (WorldEditException e) {
                    throw new RuntimeException(e);
                }
            }
        } else { // Otherwise, just make a crappy spawn with `/fill`
            Logging.error("`_pocket-world-spawn` schematic does not exist!  Making a platform using `/fill`");

            Worlds.POCKET.getWorld().setChunkForceLoaded(0, 0, true);
            Worlds.POCKET.getWorld().setChunkForceLoaded(0, -1, true);
            Worlds.POCKET.getWorld().setChunkForceLoaded(-1, 0, true);
            Worlds.POCKET.getWorld().setChunkForceLoaded(-1, -1, true);

            // Guarantee a platform for the player
            String fillCommand = "execute in minecraft:%s run fill -3 %2$d -3 3 %2$d 3 deepslate_bricks"
                .formatted(Worlds.POCKET.getName(), spawn.getBlockY() - 1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fillCommand);

            // Guarantee air
            fillCommand = "execute in minecraft:%s run fill -3 %d -3 3 %d 3 air"
                .formatted(Worlds.POCKET.getName(), spawn.getBlockY(), spawn.getBlockY() + 3);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fillCommand);

            Worlds.POCKET.getWorld().setChunkForceLoaded(0, 0, false);
            Worlds.POCKET.getWorld().setChunkForceLoaded(0, -1, false);
            Worlds.POCKET.getWorld().setChunkForceLoaded(-1, 0, false);
            Worlds.POCKET.getWorld().setChunkForceLoaded(-1, -1, false);
        }
    }

    @Override
    public void onStartup() {
        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int day = cal.get(Calendar.DAY_OF_WEEK);
        if (week % 2 == 0 && day == Calendar.MONDAY) {
            resetPocket(Bukkit.createCommandSender((comp) -> FarLands.getDebugger().echo(ComponentUtils.toText(comp))));
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (Worlds.POCKET.matches(event.getWorld())) { // Cancel all portal creation in the pocket world
            if (event.getEntity() != null) {
                event.getEntity().sendMessage(ComponentColor.red("You cannot open a portal in the pocket world."));
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!Worlds.POCKET.matches(event.getPlayer().getWorld())) return;

        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();

        if ( // Prevent Ender Portal
            event.getAction().isRightClick()
            && item != null
            && item.getType() == Material.ENDER_EYE
            && block != null
            && block.getType() == Material.END_PORTAL_FRAME
        ) {
            event.getPlayer().sendMessage(ComponentColor.red("You cannot open a portal in the pocket world."));
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (Worlds.POCKET.matches(event.getPlayer().getWorld()) && event.getCause() == PlayerSetSpawnEvent.Cause.BED) {
            event.getPlayer().sendMessage(ComponentColor.red("Your spawn point was not set because you're in the Pocket world."));
            event.setCancelled(true);
        }
    }

}
