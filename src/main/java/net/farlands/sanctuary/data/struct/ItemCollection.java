package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.data.NBTSerializer;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * A collection of items.
 */
public final class ItemCollection {

    private final Map<String, ItemStack> namedItems;
    private final GameRewardSet gameRewardSet;
    private final List<ItemReward> simpleRewards;

    /**
     *
     */
    public ItemCollection(Map<String, ItemStack> namedItems, GameRewardSet gameRewardSet, List<ItemReward> simpleRewards) {
        this.namedItems = namedItems;
        this.gameRewardSet = gameRewardSet;
        this.simpleRewards = simpleRewards != null ? new ArrayList<>(simpleRewards) : null; // Make sure it's mutable
    }

    public ItemCollection() {
        this(null, null, null);
    }

    public ItemStack getNamedItem(String name) {
        return namedItems == null ? null : namedItems.get(name);
    }

    public void onGameCompleted(Player player) {
        if (gameRewardSet != null) {
            gameRewardSet.giveReward(player);
        }
    }

    public Map<String, ItemStack> namedItems() {
        return namedItems;
    }

    public GameRewardSet gameRewardSet() {
        return gameRewardSet;
    }
    public List<ItemReward> simpleRewards() {
        return simpleRewards;
    }

    public CompoundBinaryTag toNbt() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();

        if (gameRewardSet != null) {
            nbt.put("gameRewardSet", this.gameRewardSet.toNbt());
        }

        if (namedItems != null) {
            CompoundBinaryTag.Builder namedItemsNbt = CompoundBinaryTag.builder();
            namedItems.forEach((key, value) -> namedItemsNbt.put(key, NBTSerializer.item(value)));
            nbt.put("namedItems", namedItemsNbt.build());
        }

        if (simpleRewards != null) {
            ListBinaryTag.Builder<BinaryTag> simpleRewardsNbt = ListBinaryTag.builder();
            simpleRewards.forEach(v -> simpleRewardsNbt.add(v.toNbt()));
            nbt.put("simpleRewards", simpleRewardsNbt.build());
        }

        return nbt.build();

    }

    public static ItemCollection fromNbt(CompoundBinaryTag nbt) {
        Map<String, ItemStack> namedItems = nbt.get("namedItems") != null ? new HashMap<>() : null;
        if(namedItems != null) {
            CompoundBinaryTag namedItemsNbt = (CompoundBinaryTag) nbt.get("namedItems");
            namedItemsNbt.keySet().forEach(k -> namedItems.put(k, FLUtils.itemStackFromNBT(namedItemsNbt.getByteArray(k))));
        }

        List<ItemReward> itemRewards = nbt.get("simpleRewards") != null ? new ArrayList<>() : null;
        if (itemRewards != null) {
            ListBinaryTag simpleRewardsNbt = nbt.getList("simpleRewards", CompoundBinaryTag.empty().type());
            simpleRewardsNbt.forEach(n -> itemRewards.add(ItemReward.fromNbt((CompoundBinaryTag) n)));
        }

        GameRewardSet gameRewardSet = nbt.get("gameRewardSet") != null
            ? GameRewardSet.fromNbt((CompoundBinaryTag) nbt.get("gameRewardSet"))
            : null;


        return new ItemCollection(namedItems, gameRewardSet, itemRewards);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ItemCollection) obj;
        return Objects.equals(this.namedItems, that.namedItems) &&
               Objects.equals(this.gameRewardSet, that.gameRewardSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namedItems, gameRewardSet);
    }

    @Override
    public String toString() {
        return "ItemCollection[" +
               "namedItems=" + namedItems + ", " +
               "gameRewardSet=" + gameRewardSet + ']';
    }

}
