package net.farlands.sanctuary.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatFormat;
import net.farlands.sanctuary.chat.MessageFilter;
import net.farlands.sanctuary.command.DiscordCompleter;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.command.SlashCommand;
import net.farlands.sanctuary.data.Config;
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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handler for all discord events, such as messages and commands
 */
public class DiscordHandler extends ListenerAdapter {

    private       Config.DiscordBotConfig       config;
    private final MessageChannelHandler         channelHandler;
    private       JDA                           jdaBot;
    private       boolean                       active;
    private final Map<String, DiscordCompleter> autocompleter;

    public static final String       VERIFIED_ROLE    = "Verified";
    public static final String       STAFF_ROLE       = "Staff";
    public static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "webp");

    public DiscordHandler() {
        this.config = null;
        this.channelHandler = new MessageChannelHandler();
        this.jdaBot = null;
        this.active = false;
        this.autocompleter = new HashMap<>();
    }

    /**
     * Start the discord bot with the correct configurationg
     * <br>
     * Called in {@link FarLands#onEnable()}
     */
    public void startBot() {
        config = FarLands.getFLConfig().discordBotConfig;
        try {
            if (config.token.isEmpty()) {
                Logging.error("The bot token was not set. Discord integration will not operate.");
                return;
            }

            if (config.serverID == 0L) {
                Logging.error("The serverID was not set. Discord integration will not operate.");
                return;
            }

            jdaBot = (JDABuilder.createDefault(config.token))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setAutoReconnect(true)
                .setActivity(getStats())
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this)
                .build();

        } catch (Exception ex) {
            Logging.error("Failed to setup discord jdaBot.");
            ex.printStackTrace();
        }
    }

    public JDA getNativeBot() {
        return jdaBot;
    }

    /**
     * Get a discord role by name
     */
    public Role getRole(String name) {
        if (!active) {
            return null;
        }
        List<Role> roles = getGuild().getRolesByName(name, true);
        return roles.isEmpty() ? null : roles.get(0);
    }

    /**
     * Get the discord guild
     */
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

    /**
     * Update the bot's activity
     */
    public synchronized void updateStats() {
        if (!active) {
            return;
        }

        jdaBot.getPresence().setActivity(getStats());
    }

    /**
     * Get the activity that the discord bot should show, determined based on the online players
     */
    private Activity getStats() {
        List<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers()
            .stream()
            .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
            .toList();

        String status = "with ";
        if (onlinePlayers.size() == 1) {
            status += onlinePlayers.get(0).getName();
        } else {
            status += onlinePlayers.size() + " online players";
        }

        return Activity.of(Activity.ActivityType.PLAYING, status);
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
    public void sendMessageRaw(TextChannel channel, String message) {
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
     * Get TextChannel from DiscordChannel
     */
    public TextChannel getChannel(DiscordChannel channel) {
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
     * When a new user joins the discord server
     */
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (!active) {
            return;
        }

        event
            .getUser()
            .openPrivateChannel()
            .queue((channel) -> channel.sendMessage(
                "Welcome to the FarLands official discord server! To access more channels and voice chat, " +
                "Type `/verify <minecraftUsername>` in the unverified general channel while you are on the server. You " +
                "should replace `<minecraftUsername>` with your exact minecraft username, respecting capitalization and spelling. " +
                "After doing that, type `/verify` in-game, and you're set."
            ).queue());
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        updateProposal(event.getEmoji(), event.getMessageIdLong());
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        updateProposal(event.getEmoji(), event.getMessageIdLong());
    }

    /**
     * Update a proposal when a reaction is added or removed
     */
    private void updateProposal(EmojiUnion emote, long messageId) {
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

    /**
     * Registers autocompleters for slash commands
     * @param autocompleters Map<CommandName : CompleterFunction> -- if Command Name is "*", then it will act as a backup for all commands
     */
    public void registerAutocompleters(@Nullable Map<String, DiscordCompleter> autocompleters) {
        if(autocompleters != null)
            this.autocompleter.putAll(autocompleters);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        SlashCommand slashCommand = FarLands.getCommandHandler().getSlashCommand(event.getName());
        if (slashCommand != null) {
            List<Command.Choice> autocomplete = slashCommand.autoComplete(event);
            autocomplete = autocomplete == null ? new ArrayList<>() : new ArrayList<>(autocomplete);
            Arrays.stream(
                    this.autocompleter.get("*")
                        .apply(event.getName(), event.getFocusedOption().getValue())
                )
            .map(s -> new Command.Choice(s, s))
            .forEach(autocomplete::add);
            event.replyChoices(autocomplete.stream().limit(25).toList()).queue();
            return;
        }
        List<DiscordCompleter> ac = new ArrayList<>();
        ac.add(this.autocompleter.get("*")); // Add the default autocompleter
        ac.add(this.autocompleter.get(event.getName())); // Get the autocompleter for the given command
        ac.removeIf(Objects::isNull);
        if (ac.isEmpty()) {
            event.replyChoices().queue();
            return;
        }
        AutoCompleteQuery q = event.getFocusedOption(); // Get the query
        event.replyChoiceStrings(
            ac.stream()// and reply with the correct data
                .flatMap(a -> Arrays.stream(a.apply(q.getName(), q.getValue())))
                .limit(25) // Max that Discord supports
                .toList()
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        SlashCommand slashCommand = FarLands.getCommandHandler().getSlashCommand(event);
        if (slashCommand != null) {
            try {
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getMember().getIdLong());

                // Log the command
                FarLands.getCommandHandler().logCommand(flp, event.getCommandString(), event.getChannel().asTextChannel());
                if (flp.isDiscordVerified() && flp.rank.specialCompareTo(Rank.MEDIA) >= 0) {
                    FarLands.getDiscordHandler().sendMessageRaw(
                        DiscordChannel.COMMAND_LOG,
                        flp + ": ``" + event.getCommandString().replaceAll("`", "`\u200b") + "`` (Slash Command)"
                    );
                }

                slashCommand.check(flp, event);
                slashCommand.execute(flp, event);
            } catch (SlashCommand.IllegalPermissionException ex) {
                event.reply("You do not have permission to run this command.")
                    .setEphemeral(true)
                    .queue();
            } catch (SlashCommand.CommandException ex) {
                event.reply("This was an error executing this command:\n" + ex.getMessage())
                    .setEphemeral(true)
                    .queue();
            }
            return;
        }

        String name = event.getInteraction().getFullCommandName(); // getFullCommandName gets the interaction name and the subcommand name -> /command subcommand0 subcommand1
        String command = "/" + name + " " + event.getOptions() // Convert the event data into a command that we're used to (Options go in order of registration, no matter the order of user usage)
            .stream()
            .filter(o -> !Set.of(OptionType.ATTACHMENT, OptionType.UNKNOWN).contains(o.getType()))
            .map(OptionMapping::getAsString)
            .collect(Collectors.joining(" "));

        command = command.trim(); // remove trailing spaces by having empty params

        // If the interaction has not been acknowledged after 2.5s, give it a blank message (Must be less than 3s)
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            if (!event.isAcknowledged()) {
                event.reply("\u200b").queue();
            }
        }, (long) (20 * 2.5));

        // Get a discord sender from the interaction
        DiscordSender sender = new DiscordSender(event.getInteraction());
        try {
            FarLands.getCommandHandler().handleDiscordCommand(sender, event.getInteraction(), command);
        } catch (IllegalArgumentException ex) {
            // Possible that the command is not actually valid, a removed command has yet to expire
            if (!event.isAcknowledged()) {
                event.reply("Unknown Command!").setEphemeral(true).queue();
            }
        } catch (Exception ex) {
            // If there was any error, let the sender know
            sender.ephemeral(true);
            sender.sendMessage("There was an error executing this command.");
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

        DiscordSender sender = new DiscordSender(event.getMember(), event.getChannel().asTextChannel());
        String message = event.getMessage().getContentDisplay().strip();

        if ((message.startsWith("/") || message.startsWith("\\/")) && FarLands.getCommandHandler().handleDiscordCommand(sender, event.getMessage())) {
            return;
        }
        String bodyRaw = event.getMessage().getContentStripped();
        message = MiniMessage.miniMessage().serialize(MarkdownProcessor.toMinecraft(event.getMessage().getContentRaw()));

        if (
            bodyRaw.length() > 256 && // Message is too long for in-game chat and
            channelHandler.getChannel(DiscordChannel.IN_GAME).getIdLong() == event.getChannel().getIdLong() // Message in #in-game
        ) {
            message = message.substring(0, 232).strip();

            message += MiniMessage.miniMessage().serialize(
                ComponentColor.gray("... View more on ")
                    .append(ComponentUtils.link("Discord", FarLands.getFLConfig().discordInvite))
                    .append(Component.text("."))
            );

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
                        replyHover.append(refFlp.rank).append(Component.space()); // Add rank if staff
                    }
                    replyHover.append(refFlp.rank.colorName(refFlp.username + ": ")); // Add name
                }
            }
            replyHover.append(hoverComponent);

            messagePrefix.append(ComponentColor.gray("[Reply] ").hoverEvent(HoverEvent.showText(replyHover)));
        }

        if (!event.getMessage().getAttachments().isEmpty()) { // Add the suffix of [Image] or [Attachment]
            boolean isImage = IMAGE_EXTENSIONS.contains(event.getMessage().getAttachments().get(0).getFileExtension());

            Component image = ComponentUtils.link(
                isImage ? "[Image]" : "[Attachment]",
                event.getMessage().getAttachments().get(0).getUrl(),
                NamedTextColor.GRAY
            );

            messageSuffix.append(Component.text(message.isEmpty() ? "" : " ").append(image));
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        boolean staffChat = channelHandler.getChannel(DiscordChannel.STAFF_COMMANDS).getIdLong() == event.getChannel().getIdLong();

        Component component = MiniMessage.miniMessage().deserialize(message);
        Component censorComponent = MessageFilter.INSTANCE.censor(component);

        Component finalMessage = Component.text()
            .append(messagePrefix)
            .append(component)
            .append(messageSuffix)
            .build();

        Component finalCensorMessage = Component.text()
            .append(messagePrefix)
            .append(censorComponent)
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

                TextComponent.Builder discordPrefix = Component.text()
                    .append(ComponentColor.darkGray("DISCORD "));

                if (flp.rank.isStaff()) discordPrefix.append(flp.rank).append(Component.space());

                discordPrefix
                    .append(flp.rank.colorName(flp.username))
                    .append(Component.text(": "));

                Component dPrefix = discordPrefix.build();

                Bukkit.getOnlinePlayers()
                    .stream()
                    .map(FarLands.getDataHandler()::getOfflineFLPlayer)
                    .filter(p -> !p.getIgnoreStatus(flp).includesChat())
                    .forEach(p -> p.getOnlinePlayer().sendMessage(
                        dPrefix.append(p.censoring ? finalCensorMessage : finalMessage)
                    ));

                Bukkit.getConsoleSender().sendMessage(dPrefix.append(finalMessage));
            }
        }
    }

    private Component replacements(String message, boolean silent, OfflineFLPlayer flp) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        component = ChatFormat.translatePings(component, flp, silent);
        component = ChatFormat.translateEmotes(component);
        component = ChatFormat.translateLinks(component);
        return component;
    }

    public Emoji getEmote(long id) {
        return getGuild().getEmojiById(id);
    }

    public void registerSlashCommands(List<SlashCommandData> commands) {
        this.jdaBot.updateCommands().addCommands(commands).queue();
    }
}
