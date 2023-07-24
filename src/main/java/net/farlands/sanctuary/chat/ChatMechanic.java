package net.farlands.sanctuary.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

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
        System.out.println(((TranslatableComponent) event.deathMessage()).args());
        if (flp != null && flp.getSession().deathMute) {
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
        if (event.message() == null) return;
        if (
            flp.vanished // Player is vanished
            || !event.getAdvancement().getKey().getNamespace().equalsIgnoreCase("minecraft") // or not a vanilla advancement
        ) {
            return;
        }

        TranslatableComponent msg = (TranslatableComponent) event.message();
        List<Component> args = new ArrayList<>(msg.args());
        args.set(0, flp.asComponent()); // Replace the player with the flp render
        msg = msg.args(args);

        Bukkit.getOnlinePlayers()
            .stream()
            .filter(p -> !FarLands.getDataHandler().getOfflineFLPlayer(p).getIgnoreStatus(flp).includesChat())
            .collect(Audience.toAudience())
            .sendMessage(msg);

        Advancement adv = event.getAdvancement();

        // Send advancement message to Discord
        FarLands.getDiscordHandler().sendMessageEmbed(
            DiscordChannel.IN_GAME,
            new EmbedBuilder()
                .setTitle(MarkdownProcessor.fromMinecraft(msg))
                .setDescription(
                    adv.getDisplay() == null
                        ? ""
                        : MarkdownProcessor.fromMinecraft(adv.getDisplay().description())
                )
                .setColor(
                    adv.getDisplay() == null
                        ? NamedTextColor.GREEN.value()
                        : adv.getDisplay().frame().color().value()
                )
        );
        event.message(null); // Cancel default message
    }
}
