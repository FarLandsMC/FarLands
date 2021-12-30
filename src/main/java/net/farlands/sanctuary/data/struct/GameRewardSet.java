package net.farlands.sanctuary.data.struct;

import com.kicas.rp.util.Pair;
import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Used for giving game rewards.
 */
public final class GameRewardSet {

    private final List<ItemReward> rewards;
    private final double rewardBias;
    private final boolean trackCompletionInfo;
    private final Map<UUID, Long> playerCompletionInfo;
    private final JsonItemStack finalReward;
    private final String finalRewardMessage;

    /**
     */
    public GameRewardSet(
        List<ItemReward> rewards,
        double rewardBias, boolean
            trackCompletionInfo,
        Map<UUID, Long> playerCompletionInfo,
        JsonItemStack finalReward,
        String finalRewardMessage) {
        this.rewards = rewards;
        this.rewardBias = rewardBias;
        this.trackCompletionInfo = trackCompletionInfo;
        this.playerCompletionInfo = playerCompletionInfo;
        this.finalReward = finalReward;
        this.finalRewardMessage = finalRewardMessage;
    }

    public GameRewardSet() {
        this(
            new ArrayList<>(),
            0.75,
            false,
            new HashMap<>(),
            null,
            null
        );
    }

    public void giveReward(Player player) {
        Pair<ItemStack, Integer> reward = ItemReward.randomReward(rewards, rewardBias);
        FLUtils.giveItem(player, reward.getFirst(), true);
        updateCompletionInfo(player, reward.getSecond());

        if (trackCompletionInfo && finalReward != null && hasReceivedAllRewards(player) && hasNotReceivedFinalReward(player)) {
            FLUtils.giveItem(player, finalReward.getStack(), true);
            // Change the sign bit to keep track of the final reward
            updateCompletionInfo(player, 63);

            // Send the final reward message
            if (finalRewardMessage != null) {
                try {
                    TextUtils.sendFormatted(player, finalRewardMessage);
                } catch (TextUtils.SyntaxException ex) {
                    Logging.error("Invalid final reward message encountered in game reward set:", ex.getMessage());
                }
            }
        }
    }

    private void updateCompletionInfo(Player player, int index) {
        if (trackCompletionInfo)
            playerCompletionInfo.put(player.getUniqueId(), playerCompletionInfo.getOrDefault(player.getUniqueId(), 0L) | (1L << index));
    }

    private boolean hasReceivedAllRewards(Player player) {
        long mask = (1L << rewards.size()) - 1;
        return (playerCompletionInfo.getOrDefault(player.getUniqueId(), 0L) & mask) == mask;
    }

    private boolean hasNotReceivedFinalReward(Player player) {
        // We use the sign bit to keep track of this
        return playerCompletionInfo.getOrDefault(player.getUniqueId(), 0L) >= 0;
    }

    public List<ItemReward> rewards() {
        return rewards;
    }

    public double rewardBias() {
        return rewardBias;
    }

    public boolean trackCompletionInfo() {
        return trackCompletionInfo;
    }

    public Map<UUID, Long> playerCompletionInfo() {
        return playerCompletionInfo;
    }

    public JsonItemStack finalReward() {
        return finalReward;
    }

    public String finalRewardMessage() {
        return finalRewardMessage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GameRewardSet) obj;
        return Objects.equals(this.rewards, that.rewards) &&
               Double.doubleToLongBits(this.rewardBias) == Double.doubleToLongBits(that.rewardBias) &&
               this.trackCompletionInfo == that.trackCompletionInfo &&
               Objects.equals(this.playerCompletionInfo, that.playerCompletionInfo) &&
               Objects.equals(this.finalReward, that.finalReward) &&
               Objects.equals(this.finalRewardMessage, that.finalRewardMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rewards, rewardBias, trackCompletionInfo, playerCompletionInfo, finalReward, finalRewardMessage);
    }

    @Override
    public String toString() {
        return "GameRewardSet[" +
               "rewards=" + rewards + ", " +
               "rewardBias=" + rewardBias + ", " +
               "trackCompletionInfo=" + trackCompletionInfo + ", " +
               "playerCompletionInfo=" + playerCompletionInfo + ", " +
               "finalReward=" + finalReward + ", " +
               "finalRewardMessage=" + finalRewardMessage + ']';
    }

}
