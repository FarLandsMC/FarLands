package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandToLocation extends PlayerCommand {
    public CommandToLocation() {
        super(
            CommandData.withRank(
                "tl",
                "Teleport to a location in any world.",
                "/tl <x> <y> <z> [yaw] [pitch] [world]",
                Rank.JR_BUILDER
            )
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        double x, y, z;
        float yaw, pitch;
        World world;
        if (args.length == 1 || args.length == 2 && args[0].startsWith("r.")) {
            // Tp to region
            try {
                String[] parts = args[0].split("\\.");
                x = Integer.parseInt(parts[1]) * 512;
                y = 100; // Good enough, this is only for staff :P
                z = Integer.parseInt(parts[2]) * 512;

                world = args.length == 2 ? Bukkit.getWorld(args[1]) : sender.getWorld();

                yaw = 0;
                pitch = 0;
            } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                return error(sender, "Invalid region name.");
            }
        } else if (args.length >= 3) {
            try {
                x = args[0].equals("~") ? sender.getLocation().getX() : Double.parseDouble(args[0]);
                y = args[1].equals("~") ? sender.getLocation().getY() : Double.parseDouble(args[1]);
                z = args[2].equals("~") ? sender.getLocation().getZ() : Double.parseDouble(args[2]);
                yaw = args.length <= 3 || args[3].equals("~") ? sender.getLocation().getYaw() : Float.parseFloat(args[3]);
                pitch = args.length <= 4 || args[4].equals("~") ? sender.getLocation().getPitch() : Float.parseFloat(args[4]);
            } catch (NumberFormatException ex) {
                return error(sender, "Could not find location: %s", ex.getMessage());
            }
            world = args.length >= 6 ? Bukkit.getWorld(args[5]) : sender.getWorld();
        } else {
            return false;
        }

        if (world == null) {
            return error(sender, "Invalid world: %s", args[5]);
        }

        Location newLocation = new Location(world, x, y, z, yaw, pitch);

        sender.teleport(newLocation);
        return success(sender, "Teleporting to %s.", FLUtils.toSimpleString(newLocation));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();

        List<String> worlds = Bukkit.getWorlds().stream().map(World::getName).toList();
        return switch (args.length) {
            case 2 -> args[0].startsWith("r.")
                ? worlds.stream().filter(s -> s.contains(args[1])).toList()
                : Collections.emptyList();
            case 6 -> worlds.stream().filter(s -> s.contains(args[5])).toList();
            default -> Collections.emptyList();
        };
    }
}
