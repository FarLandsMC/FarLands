package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandToLocation extends PlayerCommand {
    public CommandToLocation() {
        super(Rank.JR_BUILDER, "Teleport to a location in any world.", "/tl <x> <y> <z> [yaw] [pitch] [world]", "tl");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length < 3)
            return false;
        double x, y, z;
        float yaw, pitch;
        try {
            x = args[0].equals("~") ? sender.getLocation().getX() : Double.parseDouble(args[0]);
            y = args[1].equals("~") ? sender.getLocation().getY() : Double.parseDouble(args[1]);
            z = args[2].equals("~") ? sender.getLocation().getZ() : Double.parseDouble(args[2]);
            yaw   = args.length <= 3 || args[3].equals("~") ? sender.getLocation().getYaw()   : Float.parseFloat(args[3]);
            pitch = args.length <= 4 || args[4].equals("~") ? sender.getLocation().getPitch() : Float.parseFloat(args[4]);
        } catch (NumberFormatException ex) {
            return error(sender, "Could not find location: %s", ex.getMessage());
        }
        World world = args.length >= 6 ? Bukkit.getWorld(args[5]) : sender.getWorld();
        if (world == null) {
            return error(sender, "Invalid world: %s", args[5]);
        }
        Location newLocation = new Location(world, x, y, z, yaw, pitch);
        sender.teleport(newLocation);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length == 6
                ? Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
