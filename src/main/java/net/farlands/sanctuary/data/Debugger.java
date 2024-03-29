package net.farlands.sanctuary.data;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Methods for debugging plugin issues.
 */
public class Debugger {
    private final Map<String, Function<String[], String>> posts;

    public Debugger() {
        this.posts = new HashMap<>();
    }

    public void echo(String key, Supplier<String> data) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (FarLands.getDataHandler().getOfflineFLPlayer(player).debugging)
                player.sendMessage(ComponentColor.aqua("[DEBUG] {}: {:green}", key, data.get()));
        });
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.DEBUG, "```" + key + ": " + data.get() + "```");
        Bukkit.getConsoleSender().sendMessage(ComponentColor.aqua("[DEBUG] {}: {:green}", key, data.get()));
    }

    public void echo(String key, Object data) {
        echo(key, () -> Objects.toString(data));
    }

    public void echo(String msg) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (FarLands.getDataHandler().getOfflineFLPlayer(player).debugging)
                player.sendMessage(ComponentColor.aqua("[DEBUG] {}", msg));
        });
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.DEBUG, "```" + msg + "```");
        Bukkit.getConsoleSender().sendMessage(ComponentColor.aqua("[DEBUG] {}", msg));
    }

    public void post(String key, Function<String[], String> data) {
        posts.put(key, data);
    }

    public void post(String key, Object data) {
        posts.put(key, (unused) -> Objects.toString(data));
    }

    public Set<String> getPosts() {
        return posts.keySet();
    }

    public String getPost(String key, String... args) {
        Function<String[], String> func = posts.get(key);
        return func == null ? null : func.apply(args);
    }

    public void removePost(String key) {
        posts.remove(key);
    }
}
