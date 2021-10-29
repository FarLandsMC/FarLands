package net.farlands.sanctuary.data.struct;

import com.kicas.rp.util.TextUtils;
import com.kicas.rp.util.Pair;

import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Used for giving game rewards.
 */
public class GameRewardSet {
    private final List<ItemReward> rewards;
    private final double rewardBias;
    private final boolean trackCompletionInfo;
    private final Map<UUID, Long> playerCompletionInfo;
    private final JsonItemStack finalReward;
    private final String finalRewardMessage;

    public GameRewardSet() {
        this.rewards = new ArrayList<>();
        this.rewardBias = 0.75;
        this.trackCompletionInfo = false;
        this.playerCompletionInfo = new HashMap<>();
        this.finalReward = null;
        this.finalRewardMessage = null;
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
                    Logging.error("Invalid final reward message encountered in game reward set: " + ex.getMessage());
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
}
