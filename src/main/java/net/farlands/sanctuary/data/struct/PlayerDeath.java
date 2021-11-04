package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.util.FLUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores a player death.
 */
public record PlayerDeath(long time, Location location, int xpLevels, float xpPoints, List<ItemStack> inventory) {
    public PlayerDeath(Player player) {
        this(
            System.currentTimeMillis(), 
            player.getLocation(), 
            player.getLevel(), 
            player.getExp(),
            Arrays.asList(player.getInventory().getContents())
        );
    }

    public PlayerDeath(NBTTagCompound nbt) {
        this(
            nbt.getLong("time"),
            FLUtils.locationFromNBT(nbt.getCompound("loc")),
            nbt.getInt("xpLevels"),
            nbt.getFloat("xpPoints"),
            new ArrayList<>()
        );
        nbt.getList("inv", 10).stream().map(base -> FLUtils.itemStackFromNBT((NBTTagCompound) base)).forEach(this.inventory::add);
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
