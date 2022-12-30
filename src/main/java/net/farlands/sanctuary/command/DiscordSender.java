package net.farlands.sanctuary.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
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
public class DiscordSender implements CommandSender {

    private final DiscordSpigot           spigot;
    private final Member                  member;
    private final TextChannel             channel;
    private       SlashCommandInteraction interaction;
    private final OfflineFLPlayer         flp;
    private       boolean                 ephemeral;

    public DiscordSender(Member member, TextChannel channel) {
        this.spigot = new DiscordSpigot();
        this.member = member;
        this.channel = channel;
        this.flp = FarLands.getDataHandler().getOfflineFLPlayer(getUserID());
        this.ephemeral = false;
        this.interaction = null;
    }

    public DiscordSender(SlashCommandInteraction interaction) {
        this(interaction.getMember(), interaction.getChannel().asTextChannel());
        this.interaction = interaction;
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
        FarLands.getDiscordHandler().sendMessageRaw(channel, s);
    }

    public TextChannel getChannel() {
        return channel;
    }

    public Rank getRank() {
        return flp == null ? Rank.INITIATE : flp.rank;
    }

    public void sendMessage(String s, boolean applyFilters) {
        if (this.interaction == null || this.interaction.isAcknowledged()) {
            FarLands.getDiscordHandler().sendMessageRaw(channel, applyFilters ? MarkdownProcessor.escapeMarkdown(s) : s);
        } else {
            this.interaction
                .reply(applyFilters ? MarkdownProcessor.escapeMarkdown(s) : s)
                .setEphemeral(this.ephemeral)
                .queue();
        }
    }

    @Override
    public void sendMessage(String s) {
        sendMessage(
            MarkdownProcessor.fromMinecraft(LegacyComponentSerializer.legacySection().deserialize(s)),
            false
        );
    }

    @Override
    public void sendMessage(String[] strings) {
        sendMessage(String.join("\n", strings));
    }

    public void sendMessageEmbeds(MessageEmbed embed0, MessageEmbed... embeds) {
        if (this.interaction == null || this.interaction.isAcknowledged()) {
            this.getChannel().sendMessageEmbeds(embed0, embeds).queue();
        } else {
            this.interaction
                .replyEmbeds(embed0, embeds)
                .setEphemeral(this.ephemeral)
                .queue();
        }
    }

    public void sendFiles(FileUpload... files) {
        if (this.interaction == null || this.interaction.isAcknowledged()) {
            this.getChannel().sendFiles(files).queue();
        } else {
            this.interaction
                .replyFiles(files)
                .setEphemeral(this.ephemeral)
                .queue();
        }
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
    public void removeAttachment(PermissionAttachment permissionAttachment) {
    }

    @Override
    public void recalculatePermissions() {
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return null;
    }

    @Override
    public boolean isOp() {
        return getRank().hasOP();
    }

    @Override
    public void setOp(boolean b) {
    }

    public void sendMessage(net.minecraft.network.chat.Component component, UUID unused) {
        sendMessage(component.getString());
    }

    public void ephemeral(boolean newValue) {
        this.ephemeral = newValue;
    }

    public boolean ephemeral() {
        return this.ephemeral;
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
