package net.farlands.sanctuary.util;

import com.kicas.rp.util.TextUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * General logging methods.
 * TODO: Rewrite this class to use {@link Component}s instead of {@link BaseComponent}s.
 */
public final class Logging {
    public static void broadcastIngame(BaseComponent[] message) {
        Bukkit.getOnlinePlayers().stream().map(Player::spigot).forEach(spigot -> spigot.sendMessage(message));
        Bukkit.getConsoleSender().spigot().sendMessage(message);
    }

    /**
     * Broadcast an embed to #in-game on the Discord server
     * @param message The message to broadcast
     * @param color The color of the embed
     */
    public static void broadcastIngameDiscord(String message, Color color) {
        FarLands.getDiscordHandler().sendMessageEmbed(
            DiscordChannel.IN_GAME,
            new EmbedBuilder()
                .setTitle(MarkdownProcessor.escapeMarkdown(message))
                .setColor(ChatColor.GOLD.getColor())
        );
    }

    public static void broadcastIngameDiscord(BaseComponent[] message, Color color, String removeRegEx) {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent bc : message) {
            if (bc instanceof TextComponent)
                sb.append(((TextComponent) bc).getText());
        }
        broadcastIngameDiscord(sb.toString().replaceFirst(removeRegEx, ""), color);
    }

    public static void broadcastFormatted(String message, boolean sendToDiscord, Object... values) {
        BaseComponent[] formatted = TextUtils.format("{&(gold,bold) > }&(aqua)" + message, values);
        broadcastIngame(formatted);
        if (sendToDiscord){
            broadcastIngameDiscord(formatted, ChatColor.GOLD.getColor(), "^( > )");
        }
    }

    public static void broadcast(String input, Object... values) {
        BaseComponent[] formatted = TextUtils.format(input, values);
        broadcastIngame(formatted);
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, formatted);
    }

    public static void broadcast(Predicate<FLPlayerSession> filter, String input, Object... values) {
        BaseComponent[] formatted = TextUtils.format(input, values);
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession).filter(filter)
                .forEach(session -> session.player.spigot().sendMessage(formatted));
        Bukkit.getConsoleSender().spigot().sendMessage(formatted);
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, formatted);
    }

    public static void broadcastStaff(BaseComponent[] message, DiscordChannel discordChannel) { // Set the channel to null to not send to discord
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .forEach(session -> session.player.spigot().sendMessage(message));
        Bukkit.getConsoleSender().spigot().sendMessage(message);
        if (discordChannel != null)
            FarLands.getDiscordHandler().sendMessage(discordChannel, message);
    }

    public static void broadcastStaff(BaseComponent[] message) {
        broadcastStaff(message, null);
    }

    public static void broadcastStaffWithExemptions(Component message, Player... exemptions) {
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .filter(session -> Arrays.stream(exemptions).noneMatch(s -> s == session.player))
                .forEach(session -> session.player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static void broadcastStaff(Component message, DiscordChannel discordChannel) { // Set the channel to null to not send to discord
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .forEach(session -> session.player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
        if (discordChannel != null)
            FarLands.getDiscordHandler().sendMessage(discordChannel, message);
    }

    public static void broadcastStaff(Component message) {
        broadcastStaff(message, null);
    }

    public static void broadcastStaff(String message, DiscordChannel discordChannel) {
        broadcastStaff(LegacyComponentSerializer.legacySection().deserialize(message), discordChannel);
    }

    public static void broadcastStaff(String message) {
        broadcastStaff(TextComponent.fromLegacyText(message), null);
    }

    public static void broadcastStaffWithExemptions(BaseComponent[] message, Player exempt1, Player exempt2) {
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .filter(session -> session.player != exempt1 && session.player != exempt2)
                .forEach(session -> session.player.spigot().sendMessage(message));
        Bukkit.getConsoleSender().spigot().sendMessage(message);
    }

    public static void log(Object x) {
        Bukkit.getLogger().info("[FLv" + FarLands.getInstance().getDescription().getVersion() + "] - " + x);
    }

    public static void error(Object x) {
        String msg = Objects.toString(x);
        Bukkit.getLogger().severe("[FLv" + FarLands.getInstance().getDescription().getVersion() + "] - " + msg);
        FarLands.getDebugger().echo("Error", msg);
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, msg);
    }
}
