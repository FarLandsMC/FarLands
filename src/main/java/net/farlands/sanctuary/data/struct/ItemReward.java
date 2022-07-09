package net.farlands.sanctuary.data.struct;

import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.data.NBTSerializer;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * A reward!
 */
public class ItemReward {
    private int rarity;
    private final ItemStack stack;

    public static ItemReward fromNbt(CompoundBinaryTag tag) {
        int rarity = tag.getInt("rarity");
        byte[] stack = tag.getByteArray("stack");
        return new ItemReward(rarity, ItemStack.deserializeBytes(stack));
    }

    public ItemReward(int rarity, ItemStack stack) {
        this.rarity = rarity;
        this.stack = stack;
    }

    public ItemReward() {
        this.rarity = 0;
        this.stack = null;
    }

    public ItemStack getStack() {
        return stack;
    }

    public int getRarity() {
        return rarity;
    }

    public void setRarity(int rarity) {
        this.rarity = rarity;
    }

    // The larger the bias, the less likely a rare item is to be selected
    public static Pair<ItemStack, Integer> randomReward(List<ItemReward> rewards, double bias) {
        int index = FLUtils.biasedRandom(rewards.size(), bias);
        return new Pair<>(
            rewards.stream()
                .sorted(Comparator.comparingInt(ir -> ir.rarity))
                .toList()
                .get(index)
                .getStack()
                .clone(),
            index
        );
    }

    public BinaryTag toNbt() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        nbt.putInt("rarity", rarity);
        nbt.put("stack", NBTSerializer.item(stack));
        return nbt.build();
    }
}
