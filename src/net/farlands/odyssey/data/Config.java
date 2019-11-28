package net.farlands.odyssey.data;

import net.farlands.odyssey.discord.DiscordBotConfig;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all configuration fields for the FarLands plugin. This class is loaded and managed by the data handler.
 */
public class Config {
    private int fds; // Flight detection sensitivity, a number between 0 and 99 inclusive, 0 being least sensitive
    private int publicItems;
    private int totalItems;
    private int rotatingMessageGap;
    private int gcCycleTime;
    private long restartTime; // Time in ms since midnight UTC
    private String dedicatedMemory;
    private String screenSession;
    private String discordInvite;
    private String appealsLink;
    private String donationLink;
    private String paperDownload;
    private List<String> rotatingMessages;
    private List<String> jsUsers; // UUIDs of the people who may use /js
    private DiscordBotConfig discordBotConfig;
    private VoteConfig voteConfig;

    @SuppressWarnings("unchecked")
    public Config() {
        this.fds = 80;
        this.publicItems = 3;
        this.totalItems = 4;
        this.rotatingMessageGap = 5;
        this.gcCycleTime = 5;
        this.restartTime = 14400000L;
        this.dedicatedMemory = "1G";
        this.screenSession = "";
        this.discordInvite = "";
        this.appealsLink = "";
        this.donationLink = "";
        this.paperDownload = "";
        this.rotatingMessages = new ArrayList<>();
        this.jsUsers = new ArrayList<>();
        this.discordBotConfig = new DiscordBotConfig("", 0L,
            new Pair<>("output", 0L),
            new Pair<>("archives", 0L),
            new Pair<>("ingame", 0L),
            new Pair<>("announcements", 0L),
            new Pair<>("reports", 0L),
            new Pair<>("warpproposals", 0L),
            new Pair<>("debug", 0L),
            new Pair<>("alerts", 0L),
            new Pair<>("devreports", 0L),
            new Pair<>("suggestions", 0L),
            new Pair<>("bugreports", 0L),
            new Pair<>("staffcommands", 0L),
            new Pair<>("commandlog", 0L)
        );
        this.voteConfig = new VoteConfig();
    }

    public int getFDS() {
        return (int)Utils.constrain(fds, 0.0, 99.0);
    }

    public int getPublicItems() {
        return publicItems;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getRotatingMessageGap() {
        return rotatingMessageGap;
    }

    public int getGcCycleTime() {
        return gcCycleTime;
    }

    public long getRestartTime() {
        return restartTime;
    }

    public String getDedicatedMemory() {
        return dedicatedMemory;
    }

    public String getScreenSession() {
        return screenSession;
    }

    public boolean isScreenSessionSet() {
        return !screenSession.isEmpty();
    }

    public String getDiscordInvite() {
        return discordInvite;
    }

    public String getAppealsLink() {
        return appealsLink;
    }

    public String getDonationLink() {
        return donationLink;
    }

    public String getPaperDownload() {
        return paperDownload;
    }

    public List<String> getRotatingMessages() {
        return rotatingMessages;
    }

    public List<String> getJsUsers() {
        return jsUsers;
    }

    public DiscordBotConfig getDiscordBotConfig() {
        return discordBotConfig;
    }

    public VoteConfig getVoteConfig() {
        return voteConfig;
    }
}
