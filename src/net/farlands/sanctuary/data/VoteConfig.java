package net.farlands.sanctuary.data;

import net.farlands.sanctuary.data.struct.ItemReward;
import net.farlands.sanctuary.data.struct.JsonItemStack;

import java.util.ArrayList;
import java.util.List;

public class VoteConfig {
    public int votePartyRequirement;
    public int voteXPBoost;
    public double votePartyDistribWeight;
    public String voteLink;
    public List<JsonItemStack> voteRewards;
    public List<ItemReward> votePartyRewards;

    public VoteConfig() {
        this.votePartyRequirement = 10;
        this.voteXPBoost = 5;
        this.votePartyDistribWeight = 0.75;
        this.voteLink = "https://www.google.com/";
        this.voteRewards = new ArrayList<>();
        this.votePartyRewards = new ArrayList<>();
    }
}
