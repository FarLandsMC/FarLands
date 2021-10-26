package net.farlands.sanctuary.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Chat;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.network.chat.IChatBaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Bukkit command sender implementation fit to the discord API.
 */
public class DiscordSender implements CommandSender, ICommandListener {
    private final DiscordSpigot spigot;
    private final Member member;
    private final MessageChannel channel;
    private final OfflineFLPlayer flp;

    public DiscordSender(Member member, MessageChannel channel) {
        this.spigot = new DiscordSpigot();
        this.member = member;
        this.channel = channel;
        this.flp = FarLands.getDataHandler().getOfflineFLPlayer(getUserID());
    }

    public User getUser() {
        return member.getUser();
    }

    public Member getMember() {
        return member;
    }

    public long getUserID() {
        return member.getIdLong();
    }

    public boolean isVerified() {
        return flp != null;
    }

    public OfflineFLPlayer getFlp() {
        return flp;
    }

    public void sendMessageRaw(String s) {
        FarLands.getDiscordHandler().sendMessage(channel, s);
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public Rank getRank() {
        return flp == null ? Rank.INITIATE : flp.rank;
    }

    public void sendMessage(String s, boolean applyFilters) {
        FarLands.getDiscordHandler().sendMessage(channel, applyFilters ? Chat.applyDiscordFilters(s) : s);
    }

    @Override
    public void sendMessage(String s) {
        sendMessage(s, true);
    }

    @Override
    public void sendMessage(String[] strings) {
        sendMessage(String.join("\n", strings));
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {

    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String[] strings) {

    }

    @Override
    public @NotNull Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public @NotNull String getName() {
        return getMember().getEffectiveName();
    }

    @Override
    public @NotNull Spigot spigot() {
        return spigot;
    }

    @Override
    public @NotNull Component name() {
        return Component.text("DiscordSender");
    }

    @Override
    public boolean isPermissionSet(String s) {
        return false;
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return false;
    }

    @Override
    public boolean hasPermission(String s) {
        return isOp();
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return isOp();
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b, int i) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        return null;
    }

    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) { }

    @Override
    public void recalculatePermissions() { }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return null;
    }

    @Override
    public boolean isOp() {
        return getRank().hasOP();
    }

    @Override
    public void setOp(boolean b) { }

    @Override
    public void sendMessage(IChatBaseComponent component, UUID unused) {
        sendMessage(component.getString());
    }

    @Override
    public boolean shouldSendSuccess() {
        return true;
    }

    @Override
    public boolean shouldSendFailure() {
        return true;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return false;
    }

    @Override
    public CommandSender getBukkitSender(CommandListenerWrapper commandListenerWrapper) {
        return this;
    }

    private class DiscordSpigot extends CommandSender.Spigot {
        @Override
        public void sendMessage(BaseComponent component) {
            DiscordSender.this.sendMessage(component.toPlainText());
        }

        @Override
        public void sendMessage(BaseComponent... components) {
            DiscordSender.this.sendMessage(Stream.of(components).map(c -> c.toPlainText()).reduce("", String::concat));
        }
    }
}
