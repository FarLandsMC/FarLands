package net.farlands.sanctuary.util;

import net.minecraft.core.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Simple wrapper for {@link Location} to allow ease in serialization
 */
public class LocationWrapper {

    protected final UUID   world;
    protected       double x;
    protected       double y;
    protected       double z;
    protected       float  yaw;
    protected       float  pitch;

    public LocationWrapper(UUID world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public LocationWrapper(Location loc) {
        this(loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public Location asLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public BlockPos asBlockPos() {
        return new BlockPos((int) x, (int) y, (int) z);
    }

    public static BlockPos asBlockPos(Location location) {
        return new BlockPos((int) location.getX(), (int) location.getY(), (int) location.getZ());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (other instanceof LocationWrapper wrapper) {
            return world.equals(wrapper.world) && x == wrapper.x && y == wrapper.y && z == wrapper.z;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "x: " + FLUtils.toStringTruncated(x) + ", y: " +
               FLUtils.toStringTruncated(y) + ", z: " + FLUtils.toStringTruncated(z) +
               ", world: " + Bukkit.getWorld(world).getName();
    }
}
