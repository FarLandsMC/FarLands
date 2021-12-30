package net.farlands.sanctuary.data.struct;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;

/**
 * A collection of items.
 */
public final class ItemCollection {

    private final Map<String, JsonItemStack> namedItems;
    private final GameRewardSet gameRewardSet;

    /**
     */
    public ItemCollection(Map<String, JsonItemStack> namedItems, GameRewardSet gameRewardSet) {
        this.namedItems = namedItems;
        this.gameRewardSet = gameRewardSet;
    }

    public ItemCollection() {
        this(null, null);
    }

    public ItemStack getNamedItem(String name) {
        return namedItems == null ? null : namedItems.get(name).getStack();
    }

    public void onGameCompleted(Player player) {
        if (gameRewardSet != null)
            gameRewardSet.giveReward(player);
    }

    public Map<String, JsonItemStack> namedItems() {
        return namedItems;
    }

    public GameRewardSet gameRewardSet() {
        return gameRewardSet;
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
