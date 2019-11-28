package net.farlands.odyssey.data;

public class VoteConfig {
    private int votePartyRequirement;
    private int voteXPBoost;
    private double votePartyDistribWeight;
    private String voteLink;

    public VoteConfig() {
        this.votePartyRequirement = 10;
        this.voteXPBoost = 5;
        this.votePartyDistribWeight = 0.75;
        this.voteLink = "https://www.google.com/";
    }

    public int getVotePartyRequirement() {
        return votePartyRequirement;
    }

    public int getVoteXPBoost() {
        return voteXPBoost;
    }

    public double getVotePartyDistribWeight() {
        return votePartyDistribWeight;
    }

    public String getVoteLink() {
        return voteLink;
    }
}
