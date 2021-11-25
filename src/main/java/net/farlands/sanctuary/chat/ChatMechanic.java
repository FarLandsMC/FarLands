package net.farlands.sanctuary.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Mechanic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Mechanic for handling chat -- separate from mechanics package due to size
 */
public class ChatMechanic extends Mechanic {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ChatHandler.playerLog(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ChatHandler.playerLog(event);
    }

    /**
     * Send death message to Discord
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, event.deathMessage());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        ChatHandler.onChat(event);
    }

}
