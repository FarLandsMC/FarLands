package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

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
        if(args.length < 3)
            return false;
        double x, y, z;
        float yaw, pitch;
        try {
            x = Double.parseDouble(args[0]);
            y = Double.parseDouble(args[1]);
            z = Double.parseDouble(args[2]);
            yaw = args.length >= 4 ? Float.parseFloat(args[3]) : sender.getLocation().getYaw();
            pitch = args.length >= 5 ? Float.parseFloat(args[4]) : sender.getLocation().getPitch();
        }catch(NumberFormatException ex) {
            sendFormatted(sender, "&(red)Could not find location: " + ex.getMessage());
            return true;
        }
        World world = args.length >= 6 ? Bukkit.getWorld(args[5]) : sender.getWorld();
        if(world == null) {
            sendFormatted(sender, "&(red)Invalid world: " + args[5]);
            return true;
        }
        Location newLocation = new Location(world, x, y, z, yaw, pitch);
        sender.teleport(newLocation);
        sender.teleport(newLocation); // Required to fix a spigot glitch
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length == 6 ? Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()) : Collections.emptyList();
    }
}
