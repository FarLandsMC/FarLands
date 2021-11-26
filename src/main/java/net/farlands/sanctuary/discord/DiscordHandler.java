package net.farlands.sanctuary.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.MessageFilter;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.PluginData;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Proposal;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main discord handler.
 */
public class DiscordHandler extends ListenerAdapter {

    private DiscordBotConfig config;
    private final MessageChannelHandler channelHandler;
    private JDA jdaBot;
    private boolean active;

    public static final String VERIFIED_ROLE = "Verified";
    public static final String STAFF_ROLE = "Staff";
    public static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "webp");

    public DiscordHandler() {
        this.config = null;
        this.channelHandler = new MessageChannelHandler();
        this.jdaBot = null;
        this.active = false;
    }

    public void startBot() { // Called in FarLands#onEnable
        config = FarLands.getFLConfig().discordBotConfig;
        try {
            if (config.token.isEmpty()) {
                Logging.log("The bot token was not set. Discord integration will not operate.");
                return;
            }

            if (config.serverID == 0L) {
                Logging.log("The serverID was not set. Discord integration will not operate.");
                return;
            }

            jdaBot = (JDABuilder.createDefault(config.token))
                .setAutoReconnect(true)
                .setActivity(getStats())
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this)
                .build();

        } catch (Exception ex) {
            Logging.error("Failed to setup discord jdaBot.");
            ex.printStackTrace(System.out);
        }
    }

    public JDA getNativeBot() {
        return jdaBot;
    }

    public Role getRole(String name) {
        if (!active) {
            return null;
        }
        List<Role> roles = getGuild().getRolesByName(name, true);
        return roles.isEmpty() ? null : roles.get(0);
    }

    public Guild getGuild() {
        if (!active) {
            return null;
        }
        return jdaBot.getGuildById(config.serverID);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public synchronized void updateStats() {
        if (!active) {
            return;
        }

        jdaBot.getPresence().setActivity(getStats());
    }

    private Activity getStats() {
        List<Player> onlinePlayers = Bukkit.getOnlinePlayers()
            .stream()
            .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
            .collect(Collectors.toList());

        String status = "with ";
        if (onlinePlayers.size() == 1) {
            status += onlinePlayers.get(0).getName();
        } else {
            status += onlinePlayers.size() + " online players";
        }

        return Activity.of(Activity.ActivityType.DEFAULT, status);
    }

    @Deprecated
    public void sendMessage(DiscordChannel channel, BaseComponent[] message) {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent bc : message) {
            if (bc instanceof TextComponent) {
                sb.append(bc.toLegacyText());
            }
        }
        sendMessageRaw(channel, MarkdownProcessor.escapeMarkdown(sb.toString().replaceAll("(?i)§[0-9a-f]", "")));

    }

    /**
     * Send a message to #in-game
     */
    public void sendIngameChatMessage(Component message) {
        sendMessageRaw(
            DiscordChannel.IN_GAME,
            MarkdownProcessor.fromMinecraft(message)
        );
    }

    /**
     * Send a message directly to the specified channel (no processing)
     */
    public void sendMessageRaw(MessageChannel channel, String message) {
        channelHandler.sendMessage(channel, message);
    }

    /**
     * Send a message directly to the specified channel (no processing)
     */
    public void sendMessageRaw(DiscordChannel channel, String message) {
        if (!active) {
            return;
        }
        channelHandler.sendMessage(channel, message);
    }

    /**
     * Send a message to the specified channel
     * <p>
     * Escapes Markdown
     */
    public void sendMessage(DiscordChannel channel, String message) {
        sendMessageRaw(
            channel,
            MarkdownProcessor.escapeMarkdown(message)
        );
    }

    /**
     * Send a message to the specified channel
     * <p>
     * Converts Component -> Markdown
     */
    public void sendMessage(DiscordChannel channel, Component message) {
        sendMessageRaw(
            channel,
            MarkdownProcessor.fromMinecraft(message)
        );
    }

    /**
     * Send an embedded message to the specified channel
     *
     * @param embedBuilder Embed builder for the embed
     */
    public void sendMessageEmbed(DiscordChannel channel, EmbedBuilder embedBuilder) {
        getChannel(channel).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public void sendMessageEmbed(DiscordChannel channel, EmbedBuilder embedBuilder, Consumer<? super Message> success) {
        getChannel(channel).sendMessageEmbeds(embedBuilder.build()).queue(success);
    }

    /**
     * Get MessageChannel from DiscordChannel
     */
    public MessageChannel getChannel(DiscordChannel channel) {
        return channelHandler.getChannel(channel);
    }

    /**
     * Check if a role is controlled by the plugin
     */
    public static boolean isManagedRole(Role role) {
        return Stream.of(Rank.VALUES).anyMatch(rank -> role.getName().equals(rank.getName())) ||
            STAFF_ROLE.equals(role.getName()) || VERIFIED_ROLE.equals(role.getName());
    }

    /**
     * Setup
     */
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        config.channels.forEach((channel, id) -> channelHandler.setChannel(channel, id == 0L ? null : jdaBot.getTextChannelById(id)));
        channelHandler.startTicking();
        active = true;
    }

    /**
     * When a new user joins the server
     */
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (!active) {
            return;
        }

        event
            .getUser()
            .openPrivateChannel()
            .queue((channel) -> {
                channel.sendMessage(
                    "Welcome to the FarLands official discord server! To access more channels and voice chat, " +
                        "Type `/verify <minecraftUsername>` in the unverified general channel while you are on the server. You " +
                        "should replace `<minecraftUsername>` with your exact minecraft username, respecting capitalization and spelling. " +
                        "After doing that, type `/verify` in-game, and you're set."
                ).queue();
            });
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        updateProposal(event.getReactionEmote(), event.getMessageIdLong());
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        updateProposal(event.getReactionEmote(), event.getMessageIdLong());
    }

    /**
     * Update a proposal when a reaction is added or removed
     */
    private void updateProposal(MessageReaction.ReactionEmote emote, long messageId) {
        if (!active) {
            return;
        }

        if (!Proposal.VOTE_YES.equalsIgnoreCase(emote.getName()) &&
            !Proposal.VOTE_NO.equalsIgnoreCase(emote.getName())) {
            return;
        }
        PluginData pd = FarLands.getDataHandler().getPluginData();
        Proposal p = pd.getProposal(messageId);
        if (p != null) {
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                p.update();
                if (p.isResolved()) {
                    pd.removeProposal(p);
                }
            });
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!active) {
            return;
        }

        if (event.getAuthor().isBot()) { // Ignore bots
            return;
        }

        DiscordSender sender = new DiscordSender(event.getMember(), event.getChannel());

        // TODO: this magic
//        String[] contentRaw = event.getMessage().getContentRaw().split(" ");
//        String[] contentDisplay = event.getMessage().getContentDisplay().split(" ");
//
//        StringBuilder sb = new StringBuilder();
//
//        for (int i = 0; i < contentRaw.length; i++) {
//            String wordRaw = contentRaw[i];
//            String wordDisplay = contentDisplay[i];
//
//            if (wordRaw.matches("^<@!?(\\d+)>$")) {
//                sb.append(wordRaw).append(" ");
//                continue;
//            }
//            sb.append(wordDisplay).append(" ");
//        }
//
//        String message = sb.toString().strip();
        String message = event.getMessage().getContentRaw().strip();

        if (message.startsWith("/") && FarLands.getCommandHandler().handleDiscordCommand(sender, event.getMessage())) {
            return;
        }
        Component component = MarkdownProcessor.toMinecraft(message);
        if (
            ComponentUtils.toText(component).length() > 256 && // Message is too long for in-game chat and
                channelHandler.getChannel(DiscordChannel.IN_GAME).getIdLong() == event.getChannel().getIdLong() // Message in #in-game
        ) {
            message = message.substring(0, 232).strip();
            Component suffix = ComponentColor
                .gray("... View more on ")
                .append(
                    ComponentUtils.link(
                        "Discord",
                        FarLands.getFLConfig().discordInvite,
                        NamedTextColor.AQUA
                    )
                )
                .append(ComponentColor.gray("."));
            component = MarkdownProcessor.toMinecraft(message).append(suffix);
            // Notify sender their message was too long
            event.getMessage().reply("Your message was too long, so it was shortened for in-game chat.").queue();
        }

        TextComponent.Builder messagePrefix = Component.text(); // Goes before message
        TextComponent.Builder messageSuffix = Component.text(); // Goes after message

        Message refMessage = event.getMessage().getReferencedMessage();
        if (event.getMessage().getType() == MessageType.INLINE_REPLY && refMessage != null) { // Inline reply
            String hoverText = refMessage.getContentRaw();
            if (hoverText.length() > 60) { // Limit to 60 chars to prevent issues in chat
                hoverText = hoverText.substring(0, 60) + "...";
            }

            Component hoverComponent = MarkdownProcessor.toMinecraft(hoverText);
            OfflineFLPlayer refFlp = FarLands.getDataHandler().getOfflineFLPlayer(refMessage.getAuthor().getIdLong());

            TextComponent.Builder replyHover = Component.text();
            replyHover.color(NamedTextColor.WHITE);
            if (!refMessage.getAuthor().isBot()) { // Don't add anything if bot
                if (refFlp == null) { // "<author>: "
                    replyHover.append(ComponentColor.white(refMessage.getAuthor().getName() + ": "));
                } else { // "[rank] <name>: "
                    if (refFlp.rank.isStaff()) {
                        replyHover.append(refFlp.rank.getLabel()); // Add rank if staff
                    }
                    replyHover.append(refFlp.rank.colorName(refFlp.username + ": ")); // Add name
                }
            }
            replyHover.append(hoverComponent);

            messagePrefix.append(ComponentColor.gray("[Reply] ").hoverEvent(HoverEvent.showText(replyHover)));
        }

        if (!event.getMessage().getAttachments().isEmpty()) {
            boolean isImage = IMAGE_EXTENSIONS.contains(event.getMessage().getAttachments().get(0).getFileExtension());

            Component image = ComponentUtils.link(
                isImage ? "[Image]" : "[Attachment]",
                event.getMessage().getAttachments().get(0).getUrl(),
                NamedTextColor.GRAY
            );

            messageSuffix.append(Component.text(message.isEmpty() ? "" : " ").append(image));
        }

        boolean staffChat = channelHandler.getChannel(DiscordChannel.STAFF_COMMANDS).getIdLong() == event.getChannel().getIdLong();

        Component finalMessage = Component.text()
            .append(messagePrefix)
            .append(component)
            .append(messageSuffix)
            .build();

        if (staffChat) {
            Component scComponent =
                ComponentColor.red("[SC] ")
                    .append(ComponentColor.darkGray("DISCORD "))
                    .append(Component.text(sender.getName()))
                    .append(Component.text(": "))
                    .append(finalMessage);
            Bukkit.getConsoleSender().sendMessage(scComponent);

            FarLands.getDataHandler().getSessions().stream()
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .forEach(session -> session.player.sendMessage(scComponent.color(session.handle.staffChatColor)));

        } else if (channelHandler.getChannel(DiscordChannel.IN_GAME).getIdLong() == event.getChannel().getIdLong()) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

            if (flp != null) {
                // Handle Mute/Ban
                if (flp.isMuted() || flp.isBanned()) {
                    sender.getUser().openPrivateChannel().queue(
                        channel ->
                            channel.sendMessageEmbeds(
                                new EmbedBuilder()
                                    .setTitle("You cannot send messages through in-game chat while muted or banned.")
                                    .setColor(NamedTextColor.RED.value())
                                    .build()
                            ).queue()
                    );
                    event.getMessage().delete().queue();
                    return;
                }

                // Handle auto censor
                if (!flp.rank.isStaff() && MessageFilter.INSTANCE.autoCensor(ComponentUtils.toText(finalMessage))) {
                    sendMessageRaw(
                        DiscordChannel.ALERTS,
                        "Deleted message from in-game channel:\n```" +
                            ComponentUtils.toText(finalMessage) +
                            "```\nSent by: `" + sender.getName() + "`.");

                    event.getMessage().delete().queue();
                    return;
                }

                MiniMessage mm = MiniMessage.miniMessage();

                // May not work 100% right now, because of https://github.com/KyoriPowered/adventure-text-minimessage/issues/171
                Component censorMsg = mm.deserialize(MessageFilter.INSTANCE.censor(mm.serialize(component)));

                TextComponent.Builder discordPrefix = Component.text()
                    .append(ComponentColor.darkGray("DISCORD "));

                if (flp.rank.isStaff()) discordPrefix.append(flp.rank.getLabel()).append(Component.space());

                discordPrefix
                    .append(flp.rank.colorName(flp.username))
                    .append(Component.text(": "));

                Component fOriginal = Component.text()
                    .append(discordPrefix)
                    .append(messagePrefix)
                    .append(component)
                    .append(messageSuffix)
                    .build();

                Component fCensor = Component.text()
                    .append(discordPrefix)
                    .append(messagePrefix)
                    .append(censorMsg)
                    .append(messageSuffix)
                    .build();

                Bukkit.getOnlinePlayers()
                    .stream()
                    .map(FarLands.getDataHandler()::getOfflineFLPlayer)
                    .filter(p -> !p.getIgnoreStatus(flp).includesChat())
                    .forEach(p -> p.getOnlinePlayer().sendMessage(p.censoring ? fCensor : fOriginal));

                Bukkit.getConsoleSender().sendMessage(fOriginal);
            }
        }
    }
}
