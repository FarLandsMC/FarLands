package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.util.FLUtils;
import net.farlands.odyssey.util.Pair;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class ItemReward extends JsonItemStack implements Comparable<ItemReward> {
    private int rarity;

    public ItemReward() {
        super();
        this.rarity = 0;
    }

    @Override
    public int compareTo(ItemReward other) {
        return Integer.compare(rarity, other.rarity);
    }

    // The larger the bias, the less likely a rare item is to be selected
    public static Pair<ItemStack, Integer> randomReward(List<ItemReward> rewards, double bias) {
        int index = FLUtils.biasedRandom(rewards.size(), bias);
        return new Pair<>(
                rewards.stream().sorted(ItemReward::compareTo).collect(Collectors.toList()).get(index).getStack(),
                index
        );
    }
}
