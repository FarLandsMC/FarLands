package net.farlands.sanctuary.discord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DiscordBotConfig {
    public String token;
    public long serverID;
    public Map<DiscordChannel, Long> channels;

    public DiscordBotConfig(String token, long serverID) {
        this.token = token;
        this.serverID = serverID;
        this.channels = new HashMap<>();
        Arrays.stream(DiscordChannel.VALUES).forEach(channel -> channels.put(channel, 0L));
    }

    public DiscordBotConfig() {
        this("", 0L);
    }
}
