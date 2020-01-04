package net.farlands.odyssey.data;

import net.farlands.odyssey.FarLands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class Debugger {
    private final Map<String, Function<String[], String>> posts;

    public Debugger() {
        this.posts = new HashMap<>();
    }

    public void echo(String key, Supplier<String> data) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if(FarLands.getDataHandler().getOfflineFLPlayer(player).debugging)
                player.sendMessage(ChatColor.AQUA + "[DEBUG] " + key + ": " + ChatColor.GREEN + data.get());
        });
        FarLands.getDiscordHandler().sendMessageRaw("debug", "```" + key + ": " + data.get() + "```");
    }

    public void echo(String key, Object data) {
        echo(key, () -> Objects.toString(data));
    }

    public void echo(String msg) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if(FarLands.getDataHandler().getOfflineFLPlayer(player).debugging)
                player.sendMessage(ChatColor.AQUA + "[DEBUG] " + msg);
        });
        FarLands.getDiscordHandler().sendMessageRaw("debug", "```" + msg + "```");
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
