package net.farlands.sanctuary.data;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.ItemReward;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * FarLands voting configuration.
 */
public class VoteConfig {
    public int votePartyRequirement;
    public int voteXPBoost;
    public double votePartyDistribWeight;
    public String voteLink;

    public VoteConfig() {
        this.votePartyRequirement = 10;
        this.voteXPBoost = 5;
        this.votePartyDistribWeight = 0.75;
        this.voteLink = "https://www.google.com/";
    }

    public List<ItemStack> voteRewards() {
        return FarLands.getDataHandler().getItemList("voteRewards");
    }

    public List<ItemReward> votePartyRewards() {
        return FarLands.getDataHandler().getItemCollection("votePartyRewards").simpleRewards();
    }
}
