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
}
