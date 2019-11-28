package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class ItemReward implements Comparable<ItemReward> {
    private final ItemStack stack;
    private final int rarity;

    public ItemReward(ItemStack stack, int rarity) {
        this.stack = stack;
        this.rarity = rarity;
    }

    public ItemReward(NBTTagCompound nbt) {
        this(Utils.itemStackFromNBT(nbt.getCompound("item")), nbt.getInt("rarity"));
    }

    public NBTTagCompound asTagCompound() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.set("item", Utils.itemStackToNBT(stack));
        nbt.setInt("rarity", rarity);
        return nbt;
    }

    public ItemStack getStack() {
        return stack.clone();
    }

    @Override
    public int compareTo(ItemReward other) {
        return Integer.compare(rarity, other.rarity);
    }

    // The larger the bias, the less likely a rare item is to be selected
    public static ItemStack randomReward(List<ItemReward> rewards, double bias) {
        return rewards.stream().sorted(ItemReward::compareTo).collect(Collectors.toList()).get(Utils.biasedRandom(rewards.size(), bias)).getStack();
    }
}
