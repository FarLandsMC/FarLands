package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.UUID;

public final class Home {
    private String name;
    private UUID world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public Home(String name, UUID world, double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Home(String name, Location loc) {
        this(name, loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    Home() { }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return ChatColor.GOLD + name + ": " + ChatColor.AQUA + FLUtils.toStringTruncated(x) + ", " +
                FLUtils.toStringTruncated(y) + ", " + FLUtils.toStringTruncated(z);
    }
}
