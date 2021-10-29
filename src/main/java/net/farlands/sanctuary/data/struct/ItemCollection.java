package net.farlands.sanctuary.data.struct;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * A collection of items.
 */
public class ItemCollection {
    private final Map<String, JsonItemStack> namedItems;
    private final GameRewardSet gameRewardSet;

    public ItemCollection() {
        this.namedItems = null;
        this.gameRewardSet = null;
    }

    public ItemStack getNamedItem(String name) {
        return namedItems == null ? null : namedItems.get(name).getStack();
    }

    public void onGameCompleted(Player player) {
        if (gameRewardSet != null)
            gameRewardSet.giveReward(player);
    }
}
