package net.farlands.sanctuary.data.struct;

public enum VoteRewards {
    NONE,
    ALL,
    VP_ONLY,
    ;

    public static VoteRewards from(boolean b) {
        return b ? ALL : NONE;
    }

    /**
     * Whether this includes rewards from votes (diamonds, emeralds, steak, levels)
     */
    public boolean votes() {
        return this == ALL;
    }

    /**
     * Whether this includes rewards from vote parties
     */
    public boolean voteParties() {
        return this == ALL || this == VP_ONLY;
    }
}
