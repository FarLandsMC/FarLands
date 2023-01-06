package net.farlands.sanctuary.data;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.ItemReward;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Contains all configuration fields for the FarLands plugin. This class is loaded and managed by the data handler.
 */
public class Config {

    public int              fds; // Flight detection sensitivity, a number between 0 and 99 inclusive, 0 being least sensitive
    public int              rotatingMessageGap;
    public int              gcCycleTime; // Time between calls to System#gc
    public long             restartTime; // Time in ms since midnight UTC
    public String           dedicatedMemory; // Memory to use when restarting the server
    public String           screenSession; // Screen session to use when restarting the server
    public String           discordInvite;
    public String           appealsLink; // Invite to discord appeals channel
    public List<String>     jsUsers; // UUIDs of the people with access to /js and /artifact
    public String           donationLink;
    public DiscordBotConfig discordBotConfig;
    public VoteConfig       voteConfig;

    public Map<DataHandler.Server, String> serverRoots; // Map of server name to the directory in which the server is stored

    public Config() {
        this.fds = 80;
        this.rotatingMessageGap = 5;
        this.gcCycleTime = 5;
        this.restartTime = 14400000L;
        this.dedicatedMemory = "1G";
        this.screenSession = "";
        this.discordInvite = "";
        this.appealsLink = "";
        this.donationLink = "";
        this.jsUsers = new ArrayList<>();
        this.serverRoots = new HashMap<>();
        this.discordBotConfig = new DiscordBotConfig();
        this.voteConfig = new VoteConfig();
    }

    public int getFDS() {
        return (int) FLUtils.constrain(fds, 0.0, 99.0);
    }

    public boolean isScreenSessionNotSet() {
        return screenSession.isEmpty();
    }


    /**
     * Config for discord bot
     */
    public static class DiscordBotConfig {
        public String                    token;
        public long                      serverID;
        public Map<DiscordChannel, Long> channels;

        public DiscordBotConfig(String token, long serverID) {
            this.token = token;
            this.serverID = serverID;
            this.channels = new HashMap<>();
            Arrays.stream(DiscordChannel.VALUES).forEach(channel -> this.channels.put(channel, 0L));
        }

        public DiscordBotConfig() {
            this("", 0L);
        }
    }

    /**
     * Config for voting
     */
    public static class VoteConfig {

        public int    votePartyRequirement; // Number of votes required for vote party
        public int    voteXPBoost; // XP Levels given on vote party
        public double votePartyDistribWeight; // "This is a factor into a complicated equation that manages how vote party items are selected."
        public Map<String, String> voteLinks; // Link to voting website

        public VoteConfig() {
            this.votePartyRequirement = 10;
            this.voteXPBoost = 5;
            this.votePartyDistribWeight = 0.75;
            this.voteLinks = new HashMap<>();
        }

        public List<ItemStack> voteRewards() {
            return FarLands.getDataHandler().getItemList("voteRewards");
        }

        public List<ItemReward> votePartyRewards() {
            return FarLands.getDataHandler().getItemCollection("votePartyRewards").simpleRewards();
        }
    }
}
