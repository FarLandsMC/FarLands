package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Stores a player death.
 */
public final class PlayerDeath {

    private final long time;
    private final Location location;
    private final int xpLevels;
    private final float xpPoints;
    private final List<ItemStack> inventory;

    /**
     */
    public PlayerDeath(long time, Location location, int xpLevels, float xpPoints, List<ItemStack> inventory) {
        this.time = time;
        this.location = location;
        this.xpLevels = xpLevels;
        this.xpPoints = xpPoints;
        this.inventory = inventory;
    }

    public PlayerDeath(Player player) {
        this(
            System.currentTimeMillis(),
            player.getLocation(),
            player.getLevel(),
            player.getExp(),
            Arrays.asList(player.getInventory().getContents())
        );
    }

    public PlayerDeath(CompoundBinaryTag nbt) {
        this(
            nbt.getLong("time"),
            FLUtils.locationFromNBT(nbt.getCompound("loc")),
            nbt.getInt("xpLevels"),
            nbt.getFloat("xpPoints"),
            new ArrayList<>()
        );
        nbt.getList("inv").stream().map(base -> FLUtils.itemStackFromNBT(((ByteArrayBinaryTag) base).value())).forEach(this.inventory::add);
    }

    public CompoundBinaryTag serialize() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        nbt.putLong("time", time);
        nbt.put("loc", FLUtils.locationToNBT(location));
        nbt.putInt("xpLevels", xpLevels);
        nbt.putFloat("xpPoints", xpPoints);
        ListBinaryTag inv = ListBinaryTag.from(inventory.stream().filter(Objects::nonNull).map(FLUtils::itemStackToNBT).map(ByteArrayBinaryTag::of).toList());
        nbt.put("inv", inv);
        return nbt.build();
    }

    public long time() {
        return time;
    }

    public Location location() {
        return location;
    }

    public int xpLevels() {
        return xpLevels;
    }

    public float xpPoints() {
        return xpPoints;
    }

    public List<ItemStack> inventory() {
        return inventory;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlayerDeath) obj;
        return this.time == that.time &&
               Objects.equals(this.location, that.location) &&
               this.xpLevels == that.xpLevels &&
               Float.floatToIntBits(this.xpPoints) == Float.floatToIntBits(that.xpPoints) &&
               Objects.equals(this.inventory, that.inventory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, location, xpLevels, xpPoints, inventory);
    }

    @Override
    public String toString() {
        return "PlayerDeath[" +
               "time=" + time + ", " +
               "location=" + location + ", " +
               "xpLevels=" + xpLevels + ", " +
               "xpPoints=" + xpPoints + ", " +
               "inventory=" + inventory + ']';
    }

}
