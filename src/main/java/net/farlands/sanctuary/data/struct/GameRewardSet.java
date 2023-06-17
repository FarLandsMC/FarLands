package net.farlands.sanctuary.data.struct;

import com.kicas.rp.util.Pair;
import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Used for giving game rewards.
 */
public record GameRewardSet(
    List<ItemReward> rewards,
    double rewardBias,
    boolean trackCompletionInfo,
    Map<UUID, Long> playerCompletionInfo,
    ItemStack finalReward,
    String finalRewardMessage
) {

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

    public static GameRewardSet fromNbt(CompoundBinaryTag nbt) {
        ListBinaryTag rewardsNbt = nbt.getList("rewards");
        List<ItemReward> rewards = rewardsNbt
            .stream()
            .map(bt -> (CompoundBinaryTag) bt)
            .map(ItemReward::fromNbt)
            .collect(Collectors.toList());
        return new GameRewardSet(
            rewards,
            nbt.getDouble("rewardBias"),
            nbt.getBoolean("trackCompletionInfo"),
            playerCompletionInfoFromNbt(nbt.get("playerCompletionInfo")),
            FLUtils.itemStackFromNBT(nbt.getByteArray("finalReward")),
            nbt.getString("finalRewardMessage")
        );


    }

    private static Map<UUID, Long> playerCompletionInfoFromNbt(BinaryTag playerCompletionInfo) {
        Map<UUID, Long> map = new HashMap<>();
        CompoundBinaryTag nbt = (CompoundBinaryTag) playerCompletionInfo;
        nbt.keySet().forEach(k -> map.put(UUID.fromString(k), nbt.getLong(k)));
        return map;
    }

    public void giveReward(Player player) {
        Pair<ItemStack, Integer> reward = ItemReward.randomReward(rewards, rewardBias);
        FLUtils.giveItem(player, reward.getFirst(), true);
        updateCompletionInfo(player, reward.getSecond());

        if (trackCompletionInfo && finalReward != null && hasReceivedAllRewards(player) && hasNotReceivedFinalReward(player)) {
            FLUtils.giveItem(player, finalReward, true);
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
        if (trackCompletionInfo) {
            playerCompletionInfo.put(player.getUniqueId(), playerCompletionInfo.getOrDefault(player.getUniqueId(), 0L) | (1L << index));
        }
    }

    private boolean hasReceivedAllRewards(Player player) {
        long mask = (1L << rewards.size()) - 1;
        return (playerCompletionInfo.getOrDefault(player.getUniqueId(), 0L) & mask) == mask;
    }

    private boolean hasNotReceivedFinalReward(Player player) {
        // We use the sign bit to keep track of this
        return playerCompletionInfo.getOrDefault(player.getUniqueId(), 0L) >= 0;
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
    public String toString() {
        return "GameRewardSet[" +
               "rewards=" + rewards + ", " +
               "rewardBias=" + rewardBias + ", " +
               "trackCompletionInfo=" + trackCompletionInfo + ", " +
               "playerCompletionInfo=" + playerCompletionInfo + ", " +
               "finalReward=" + finalReward + ", " +
               "finalRewardMessage=" + finalRewardMessage + ']';
    }

    private CompoundBinaryTag playerCompletionInfoAsNbt() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        playerCompletionInfo.forEach((key, value) -> nbt.putLong(key.toString(), value));

        return nbt.build();
    }

    public BinaryTag toNbt() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();

        nbt.put(
            "rewards",
            ListBinaryTag.of(
                CompoundBinaryTag.empty().type(),
                rewards.stream().map(ItemReward::toNbt).toList())
        );
        nbt.putDouble("rewardBias", rewardBias);
        nbt.putBoolean("trackCompletionInfo", trackCompletionInfo);
        nbt.put("playerCompletionInfo", playerCompletionInfoAsNbt());
        nbt.putByteArray("finalReward", FLUtils.itemStackToNBT(finalReward));
        nbt.putString("finalRewardMessage", finalRewardMessage);

        return nbt.build();
    }
}
