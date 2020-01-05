package net.farlands.odyssey.util;

import net.minecraft.server.v1_15_R1.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * This class will play nice with Gson.
 */
public final class LocationWrapper {
    private final String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public LocationWrapper(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public LocationWrapper(Location loc) {
        this(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public Location asLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public BlockPosition asBlockPosition() {
        return new BlockPosition(x, y, z);
    }

    public static BlockPosition asBlockPosition(Location location) {
        return new BlockPosition(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public boolean equals(Object other) {
        if(other == this)
            return true;
        if(other == null)
            return false;
        if(other instanceof LocationWrapper) {
            LocationWrapper wrapper = (LocationWrapper)other;
            return world.equals(wrapper.world) && x == wrapper.x && y == wrapper.y && z == wrapper.z;
        }else
            return false;
    }

    @Override
    public String toString() {
        return (new StringBuilder()).append("x: ").append(FLUtils.toStringTruncated(x)).append(", y: ")
                .append(FLUtils.toStringTruncated(y)).append(", z: ").append(FLUtils.toStringTruncated(z))
                .append(", world: ").append(world).toString();
    }
}
