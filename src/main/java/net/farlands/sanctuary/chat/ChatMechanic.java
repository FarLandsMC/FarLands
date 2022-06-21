package net.farlands.sanctuary.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Mechanic;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
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
     * Cancel the death message if `deathMute` is enabled for the player's session, otherwise send to Discord
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getEntity());
        if(flp != null && flp.getSession().deathMute) {
            event.deathMessage(null);
        } else {
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, event.deathMessage());
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        ChatHandler.onChat(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        if(event.message() == null) return;
        if (
            flp.vanished // Player is vanished
            || !event.getAdvancement().getKey().getNamespace().equalsIgnoreCase("minecraft") // or not a vanilla advancement
        ) {
            event.message(null); // Make no message
            return;
        }

        Bukkit.getOnlinePlayers()
            .stream()
            .filter(p -> !FarLands.getDataHandler().getOfflineFLPlayer(p).getIgnoreStatus(flp).includesChat())
            .forEach(p -> p.sendMessage(event.message()));

        // Send advancement message to Discord
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, event.message());

    }
}
