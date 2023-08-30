package net.farlands.sanctuary.chat;

import io.papermc.paper.advancement.AdvancementDisplay;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
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
        if (flp != null && flp.getSession().deathMute) {
            event.deathMessage(null);
            return;
        } else {
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, event.deathMessage());
        }

        event.deathMessage(ComponentUtils.convertPlayers(event.deathMessage()));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        ChatHandler.onChat(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        if (event.message() == null) return;
        if (
            flp.vanished // Player is vanished
            || !event.getAdvancement().getKey().getNamespace().equalsIgnoreCase("minecraft") // or not a vanilla advancement
        ) {
            return;
        }

        Component msg = ComponentUtils.convertPlayers(event.message());
        assert msg != null; // We check for null above and convertPlayers can't return null

        Bukkit.getOnlinePlayers()
            .stream()
            .filter(p -> !FarLands.getDataHandler().getOfflineFLPlayer(p).getIgnoreStatus(flp).includesChat())
            .collect(Audience.toAudience())
            .sendMessage(msg);

        Advancement adv = event.getAdvancement();

        // Send advancement message to Discord
        AdvancementDisplay display = adv.getDisplay();
        if (display != null) {
            FarLands.getDiscordHandler().sendMessageEmbed(
                DiscordChannel.IN_GAME,
                new EmbedBuilder()
                    .setTitle(MarkdownProcessor.fromMinecraft(msg))
                    .setDescription(MarkdownProcessor.fromMinecraft(adv.getDisplay().description()))
                    .setColor(adv.getDisplay().frame().color().value())
            );
        }
        event.message(null); // Cancel default message
    }
}
