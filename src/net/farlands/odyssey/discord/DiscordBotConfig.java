package net.farlands.odyssey.discord;

import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;

import java.util.Map;

public class DiscordBotConfig {
    public String token;
    public long serverID;
    public Map<String, Long> channels;

    @SafeVarargs
    public DiscordBotConfig(String token, long serverID, Pair<String, Long>... channels) {
        this.token = token;
        this.serverID = serverID;
        this.channels = Utils.asMap(channels);
    }

    public DiscordBotConfig() {
        this("", 0L);
    }
}
