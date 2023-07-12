package net.farlands.sanctuary.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Util class for logging information to console, Discord, and staff
 */
public class Logging {

    private static final MiniMessage          MM      = MiniMessage.miniMessage();
    private static final ConsoleCommandSender CONSOLE = Bukkit.getConsoleSender();

    public static final Component PREFIX = ComponentColor.gold(" > ").decorate(TextDecoration.BOLD); // Prefix for global in-game announcements

    /**
     * Send info level log to the logger
     */
    public static void log(Object... objects) {
        Bukkit.getLogger().info(Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(" ")));
    }

    /**
     * Send severe level log to the logger, send a debug message, and log into #scribes-notebook
     */
    public static void error(Object... objects) {
        String msg = Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(" "));
        Bukkit.getLogger().severe(msg);
        FarLands.getDebugger().echo("Error", msg);
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, msg);
    }

    /**
     * Broadcast a message to all players ingame, with the option to send to Discord (#in-game)
     * <p>
     * Parses MiniMessage
     */
    public static void broadcastIngame(String message, boolean sendToDiscord) {
        Component c = MM.deserialize(message);
        broadcastIngame(c, sendToDiscord);
    }


    /**
     * Broadcast a message to all players ingame, with the option to send to Discord (#in-game)
     */
    public static void broadcastIngame(Component message, boolean sendToDiscord) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        CONSOLE.sendMessage(message);

        if (sendToDiscord) {
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, message);
        }
    }

    /**
     * Broadcast a message to #in-game on the Discord
     */
    public static void broadcastDiscord(String message) {
        broadcastDiscord(message, DiscordChannel.IN_GAME);
    }

    /**
     * Broadcast a message to a specified Discord channel
     */
    public static void broadcastDiscord(String message, DiscordChannel channel) {
        FarLands.getDiscordHandler().sendMessageEmbed(
            channel,
            new EmbedBuilder()
                .setTitle(message)
                .setColor(NamedTextColor.GOLD.value())
        );
    }

    /**
     * Send a formatted message to Discord (#in-game) and in-game chat
     * <p>
     * Parses MiniMessage
     *
     * @param message Message, formatted with {@link String#format(String, Object...)}
     */
    public static void broadcastFormatted(String message, boolean sendToDiscord, Object... replacements) {
        Component c = MM.deserialize(String.format(message, replacements));
        if (sendToDiscord) {
            broadcastDiscord(MarkdownProcessor.fromMinecraft(c), DiscordChannel.IN_GAME);
        }
        broadcastIngame(PREFIX.append(c), false);
    }

    /**
     * Send a formatted message to players that match the predicate
     *
     * @param filter  Predicate to filter players
     * @param message Message, formatted with {@link String#format(String, Object...)}
     */
    public static void broadcast(Predicate<FLPlayerSession> filter, String message, Object... replacements) {
        Component c = MM.deserialize(String.format(message, replacements));
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession).filter(filter).forEach(s -> {
            s.player.sendMessage(c);
        });
        CONSOLE.sendMessage(c);
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, c);
    }

    /**
     * Send a message to staff members, as well as Discord(unless null)
     *
     * @param channel Channel to send message to -- null to not send
     */
    public static void broadcastStaff(Component message, DiscordChannel channel) {
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
            .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
            .forEach(session -> session.player.sendMessage(message));
        CONSOLE.sendMessage(message);
        if (channel != null) {
            FarLands.getDiscordHandler().sendMessage(channel, message);
        }
    }

    public static void broadcastStaff(Component message) {
        broadcastStaff(message, null);
    }

    /**
     * Send a message to online staff, excluding the exemptions
     *
     * @param exemptions Players to exempt from the broadcast
     */
    public static void broadcastStaffExempt(Component message, CommandSender... exemptions) {
        List<CommandSender> exempt = List.of(exemptions);
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
            .filter(
                session ->
                    session.handle.rank.isStaff() &&
                    session.showStaffChat &&
                    !exempt.contains(session.player)
            )
            .forEach(session -> session.player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * Send a message to online staff [and discord]
     *
     * @param message Message to send -- formatted with minimessage
     * @param channel Channel to send message to -- null to not send
     */
    public static void broadcastStaff(String message, DiscordChannel channel) {
        broadcastStaff(MM.deserialize(message), channel);
    }

    /**
     * Send a message to online staff [and discord]
     *
     * @param message Message to send -- formatted with minimessage
     */
    public static void broadcastStaff(String message) {
        broadcastStaff(MM.deserialize(message));
    }
}
