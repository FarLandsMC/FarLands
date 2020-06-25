package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.util.FLUtils;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.NBTTagList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerDeath {
    private final long time;
    private final Location location;
    private final int xpLevels;
    private final float xpPoints;
    private final List<ItemStack> inventory;

    public PlayerDeath(long time, Location location, int xpLevels, float xpPoints, List<ItemStack> inventory) {
        this.time = time;
        this.location = location;
        this.xpLevels = xpLevels;
        this.xpPoints = xpPoints;
        this.inventory = inventory;
    }

    public PlayerDeath(Player player) {
        this(System.currentTimeMillis(), player.getLocation(), player.getLevel(), player.getExp(), Arrays.asList(player.getInventory().getContents()));
    }

    public PlayerDeath(NBTTagCompound nbt) {
        this.time = nbt.getLong("time");
        this.location = FLUtils.locationFromNBT(nbt.getCompound("loc"));
        this.xpLevels = nbt.getInt("xpLevels");
        this.xpPoints = nbt.getFloat("xpPoints");
        this.inventory = new ArrayList<>();
        nbt.getList("inv", 10).stream().map(base -> FLUtils.itemStackFromNBT((NBTTagCompound)base)).forEach(this.inventory::add);
    }

    public long getTime() {
        return time;
    }

    public Location getLocation() {
        return location;
    }

    public int getXpLevels() {
        return xpLevels;
    }

    public float getXpPoints() {
        return xpPoints;
    }

    public List<ItemStack> getInventory() {
        return inventory;
    }

    public NBTTagCompound serialize() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("time", time);
        nbt.set("loc", FLUtils.locationToNBT(location));
        nbt.setInt("xpLevels", xpLevels);
        nbt.setFloat("xpPoints", xpPoints);
        NBTTagList inv = new NBTTagList();
        inventory.stream().map(FLUtils::itemStackToNBT).forEach(inv::add);
        nbt.set("inv", inv);
        return nbt;
    }
}
