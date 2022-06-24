package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandPocketReset extends Command {

    private final Set<OfflineFLPlayer> needsConfirm;

    public CommandPocketReset() {
        super(Rank.ADMIN, Category.STAFF, "Remove all set homes from the pocket world and set logout " +
                                          "locations from that world to spawn.", "/resetpocket [confirm]", "resetpocket"); // Not /pocketreset to prevent accidental tab-complete when doing /pocket
        needsConfirm = new HashSet<>();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        int playersWithHomes = 0;
        int removedHomes = 0;
        int movedPlayers = 0;
        if (
            this.needsConfirm.contains(FarLands.getDataHandler().getOfflineFLPlayer(sender)) &&
            args.length > 0 &&
            args[0].equalsIgnoreCase("confirm")
        ) {
            for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {

                success(sender, "Resetting Pocket World...");
                // Remove all homes in the party world - this does work
                List<Home> partyHomes = flp.homes // Get all homes in party world
                    .stream()
                    .filter(home -> Worlds.POCKET.matches(home.getLocation().getWorld()))
                    .toList();

                partyHomes.forEach(home -> flp.removeHome(home.getName())); // Remove the homes

                if (partyHomes.size() > 0) ++playersWithHomes; // Add to the amount of players with changed homes
                removedHomes += partyHomes.size(); // Add to the amount of removed homes

                // Teleport them to spawn if needed
                if (Worlds.POCKET.matches(flp.lastLocation.asLocation().getWorld())) {
                    flp.moveToSpawn();
                    ++movedPlayers;
                }


            }
            this.needsConfirm.remove(FarLands.getDataHandler().getOfflineFLPlayer(sender));

            FarLands.getDataHandler().getPluginData().pocketSpawn = null;

            success(sender, "Deleted Pocket World spawn.");

            info(
                sender,
                "Removed %s %s from %s %s and moved %s %s to server spawn.",
                removedHomes, removedHomes == 1 ? "home" : "homes",
                playersWithHomes, playersWithHomes == 1 ? "player" : "players",
                movedPlayers, movedPlayers == 1 ? "player" : "players"

            );
            success(sender, "Attempting to delete world files...");
            long start = System.currentTimeMillis();
            byte[] uidDat; // uid.dat file -- saved to keep the UUID of the world
            File worldFolder = Worlds.POCKET.getWorld().getWorldFolder();
            try {
                Bukkit.unloadWorld(Worlds.POCKET.getWorld(), false);

                // Save uid.dat file
                uidDat = Files.readAllBytes(Paths.get(worldFolder.getAbsolutePath(), "uid.dat"));

                FileUtils.deleteDirectory(worldFolder);
                success(sender, "Deleted pocket world files in %s.", TimeInterval.formatTime(System.currentTimeMillis() - start, true));
            } catch (Exception e) {
                error(sender, "Unable to delete the pocket world folder");
                return true;
            }

            success(sender, "Loading new pocket world, expect a lot of lag...");
            start = System.currentTimeMillis();
            try {
                // Restore uid.dat file
                worldFolder.mkdirs();
                FileOutputStream fos = new FileOutputStream(Paths.get(worldFolder.getAbsolutePath(), "uid.dat").toFile());
                fos.write(uidDat);
                fos.close();

                Worlds.POCKET.createWorld();

                WorldBorder wb = Worlds.POCKET.getWorld().getWorldBorder();
                wb.setCenter(0.5, 0.5);
                wb.setSize(5000 * 2 + 1); // Diameter + 1 for centre block

            } catch (Exception e) {
                error(sender, "Unable to load the new pocket world");
                return true;
            }

            sender.sendMessage(
                ComponentColor.green(
                    "Loaded new pocket world in %s (Don't forget to set the new spawn!) - ",
                    TimeInterval.formatTime(System.currentTimeMillis() - start, true)
                ).append(
                    ComponentUtils.command(
                        "/chain {gm3} {tl 0 100 0 ~ ~ pocket}",
                        ComponentColor.aqua("Click here to teleport")
                    )
                )
            );
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.NOTEBOOK, sender.getName() + " reset the pocket world.");


        } else {
            sender.sendMessage(
                ComponentColor.red("Are you sure that you want to run this command? It will delete all homes in the pocket world and teleport all players in the pocket world to spawn.  It will also ")
                    .append(Component.text("delete", Style.style(NamedTextColor.RED, TextDecoration.BOLD)))
                    .append(ComponentColor.red(" the pocket world and generate a new one. If you are certain that you want to do this, run "))
                    .append(ComponentColor.darkRed("/resetpocket confirm"))
                    .append(ComponentColor.red("."))
            );
            this.needsConfirm.add(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                this.needsConfirm.remove(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            }, 30 * 20);
        }


        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length != 0) return Collections.emptyList();
        return Collections.singletonList("confirm");
    }
}