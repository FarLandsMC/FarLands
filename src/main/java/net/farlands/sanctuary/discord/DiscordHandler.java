package net.farlands.sanctuary.discord;

import com.kicas.rp.util.TextUtils2;
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
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.PluginData;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Proposal;
import net.farlands.sanctuary.discord.markdown.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.Logging;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
        if (!active)
            return null;
        List<Role> roles = getGuild().getRolesByName(name, true);
        return roles == null || roles.isEmpty() ? null : roles.get(0);
    }

    public Guild getGuild() {
        if (!active)
            return null;
        return jdaBot.getGuildById(config.serverID);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public synchronized void updateStats() {
        if (!active)
            return;

        jdaBot.getPresence().setActivity(getStats());
    }

    private Activity getStats() {
        List<Player> onlinePlayers = Bukkit.getOnlinePlayers()
            .stream()
            .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
            .collect(Collectors.toList());

        String status = "with ";
        if (onlinePlayers.size() == 1)
            status += onlinePlayers.iterator().next().getName();
        else
            status += onlinePlayers.size() + " online players";

        return Activity.of(Activity.ActivityType.DEFAULT, status);
    }

    public void sendIngameChatMessage(BaseComponent[] message, int start) {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent bc : message) {
            if (bc instanceof TextComponent)
                sb.append(bc.toLegacyText());
        }
        sendMessageRaw(DiscordChannel.IN_GAME, Chat.applyDiscordFilters(sb.toString().replaceAll("(?i)§[0-9a-f]", ""), start));
    }

    public void sendMessage(MessageChannel channel, String message) {
        channelHandler.sendMessage(channel, message);
    }

    public void sendMessageRaw(DiscordChannel channel, String message) {
        if (!active)
            return;
        channelHandler.sendMessage(channel, message);
    }

    public void sendMessage(DiscordChannel channel, String message) {
        sendMessageRaw(channel, Chat.applyDiscordFilters(message));
    }

    public void sendMessage(DiscordChannel channel, BaseComponent[] message) {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent bc : message) {
            if (bc instanceof TextComponent)
                sb.append(((TextComponent) bc).getText());
        }
        sendMessage(channel, sb.toString());
    }

    public void sendMessageEmbed(DiscordChannel channel, EmbedBuilder embedBuilder) {
        getChannel(channel).sendMessage(embedBuilder.build()).queue();
    }

    public void sendMessageEmbed(DiscordChannel channel, EmbedBuilder embedBuilder, Consumer<? super Message> success) {
        getChannel(channel).sendMessage(embedBuilder.build()).queue(success);
    }

    public MessageChannel getChannel(DiscordChannel channel) {
        return channelHandler.getChannel(channel);
    }

    public static boolean isManagedRole(Role role) {
        return Stream.of(Rank.VALUES).anyMatch(rank -> role.getName().equals(rank.getName())) ||
                STAFF_ROLE.equals(role.getName()) || VERIFIED_ROLE.equals(role.getName());
    }

    @Override
    public void onReady(ReadyEvent event) {
        config.channels.forEach((channel, id) -> channelHandler.setChannel(channel, id == 0L ? null : jdaBot.getTextChannelById(id)));
        channelHandler.startTicking();
        active = true;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (!active)
            return;

        event.getUser().openPrivateChannel().queue((channel) -> {
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
        if (!active)
            return;

        if (!Proposal.VOTE_YES.equalsIgnoreCase(event.getReactionEmote().getName()) &&
                !Proposal.VOTE_NO.equalsIgnoreCase(event.getReactionEmote().getName()))
            return;
        PluginData pd = FarLands.getDataHandler().getPluginData();
        Proposal p = pd.getProposal(event.getMessageIdLong());
        if (p != null) {
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                p.update();
                if (p.isResolved())
                    pd.removeProposal(p);
            });
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (!active)
            return;

        if (!Proposal.VOTE_YES.equalsIgnoreCase(event.getReactionEmote().getName()) &&
                !Proposal.VOTE_NO.equalsIgnoreCase(event.getReactionEmote().getName()))
            return;
        PluginData pd = FarLands.getDataHandler().getPluginData();
        Proposal p = pd.getProposal(event.getMessageIdLong());
        if (p != null) {
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                p.update();
                if (p.isResolved())
                    pd.removeProposal(p);
            });
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!active)
            return;

        if (event.getAuthor().isBot())
            return;

        DiscordSender sender = new DiscordSender(event.getMember(), event.getChannel());

        String[] contentRaw = event.getMessage().getContentRaw().split(" ");
        String[] contentDisplay = event.getMessage().getContentDisplay().split(" ");

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < contentRaw.length; i++) {
            String wordRaw = contentRaw[i];
            String wordDisplay = contentDisplay[i];

            if (wordRaw.matches("^<@!?(\\d+)>$")) {
                sb.append(wordRaw).append(" ");
                continue;
            }
            sb.append(wordDisplay).append(" ");
        }

        String message = sb.toString().strip();

        if (message.startsWith("/") && FarLands.getCommandHandler().handleDiscordCommand(sender, event.getMessage()))
            return;
        message = TextUtils2.escapeExpression(Chat.removeColorCodes(message));
        message = MarkdownProcessor.markdownToMC(message);
        message = message.trim();
        if (message.length() > 256 && (channelHandler.getChannel(DiscordChannel.IN_GAME).getIdLong()
                == event.getChannel().getIdLong())) {
            message = message.substring(0, 232);
            message = message.trim() + "&(gray)... View more on {" +
                "$(click:open_url," + FarLands.getFLConfig().discordInvite + ")" +
                "$(hover:show_text,&(gold)Click to join our Discord server.)" +
                "&(aqua,underline)Discord" +
                "}";
            // Notify sender their message was too long
            event.getMessage().reply("Your message was too long, so it was shortened for in-game chat.").queue();
        }

        String prefix = "";
        Message refMessage = event.getMessage().getReferencedMessage();
        if (event.getMessage().getType() == MessageType.INLINE_REPLY && refMessage != null) {
            String hoverText = refMessage.getContentDisplay();
            if (hoverText.length() > 60) { // Limit to 60 chars to prevent issues in chat
                hoverText = hoverText.substring(0, 60) + "...";
            }
            hoverText = hoverText.replaceAll(",", "");
            hoverText = Chat.removeColorCodes(hoverText);

            OfflineFLPlayer refFlp = FarLands.getDataHandler().getOfflineFLPlayer(refMessage.getAuthor().getIdLong());


            String pf;
            if (refMessage.getAuthor().isBot()) {
                pf = "&(white)";
            } else if (refFlp == null) {
                pf = "&(white)" + refMessage.getAuthor().getName() + ": ";
            } else {
                pf = "&(" + (refFlp.rank.isStaff() ? "bold," : "") +
                    refFlp.rank.getColor().getName() + ")" +
                    refFlp.rank.getName() + "&(!bold) " +
                    refFlp.username + ":&(white) ";
            }

            hoverText = TextUtils2.escapeExpression(pf + hoverText);

            prefix = "{$(hover:show_text," + hoverText + ")&(gray,bold)[Reply]} ";
        }
        message = prefix + message;
        message = message.replaceAll("\\\\", ""); // Replace single \s

        if (!event.getMessage().getAttachments().isEmpty()) {
            if (message.isEmpty())
                message = "";
            else
                message += " ";

            message += "{" +
                "$(hover:show_text,&(aqua)Open image URL)" +
                "$(click:open_url," + event.getMessage().getAttachments().get(0).getUrl() + ")" +
                "&(gray,bold)[Image]" +
            "}";
        }

        boolean staffChat = channelHandler.getChannel(DiscordChannel.STAFF_COMMANDS).getIdLong() == event.getChannel().getIdLong();

        final String fmessage = Chat.atPlayer(
            Chat.limitFlood(Chat.limitCaps(message)), sender.getFlp().uuid,
            channelHandler.getChannel(DiscordChannel.IN_GAME).getIdLong() != event.getChannel().getIdLong()
        );

        if (staffChat) {
            try {
                TextUtils2.sendFormatted(
                    Bukkit.getConsoleSender(),
                    "&(red)[SC] {&(dark_gray,bold)DISCORD} %0: %1",
                    sender.getName(),
                    fmessage
                );
            } catch (TextUtils2.ParserError e) {
                e.printStackTrace();
            }

            FarLands.getDataHandler().getSessions().stream()
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .forEach(session -> {
                    try {
                        TextUtils2.sendFormatted(
                            session.player,
                            "&(%0)[SC] {&(dark_gray)DISCORD} %1: %2",
                            session.handle.staffChatColor.name(),
                            sender.getName(),
                            fmessage
                        );
                    } catch (TextUtils2.ParserError parserError) {
                        parserError.printStackTrace();
                    }
                });
        }

        else if (channelHandler.getChannel(DiscordChannel.IN_GAME).getIdLong() == event.getChannel().getIdLong()) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            if (flp != null) {
                if (flp.isMuted() || flp.isBanned()) {
                    sender.getUser().openPrivateChannel().queue(channel ->
                            channel.sendMessage("You cannot send messages through in-game chat while muted or banned.").queue()
                    );
                    event.getMessage().delete().queue();
                    return;
                }

                Rank rank = flp.rank;

                if (!rank.isStaff() && Chat.getMessageFilter().autoCensor(fmessage)) {
                    sendMessageRaw(DiscordChannel.ALERTS, "Deleted message from in-game channel:\n```" + fmessage +
                            "```\nSent by: `" + sender.getName() + "`.");
                    event.getMessage().delete().queue();
                    return;
                }

                String censorMessage = Chat.getMessageFilter().censor(fmessage);
                Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(p -> !FarLands.getDataHandler().getOfflineFLPlayer(p).getIgnoreStatus(flp).includesChat())
                    .forEach(p ->
                             {
                                 try {
                                     TextUtils2.sendFormatted(
                                         p,
                                         "&(dark_gray)DISCORD &(%1){%0%2}%3: &(white)%4",
                                         rank.isStaff() ? "&(bold)" : "",
                                         rank.getNameColor().getName(),
                                         rank.isStaff() ? rank.getName() + " " : "",
                                         flp.username,
                                         FarLands.getDataHandler().getOfflineFLPlayer(p).censoring ? censorMessage : fmessage
                                     );
                                 } catch (TextUtils2.ParserError parserError) {
                                     parserError.printStackTrace();
                                 }
                             }
                    );

                try {
                    TextUtils2.sendFormatted(
                        Bukkit.getConsoleSender(),
                        "&(dark_gray)DISCORD &(%1){%0%2}%3: &(white)%4",
                        rank.isStaff() ? "&(bold)" : "",
                        rank.getNameColor().getName(),
                        rank.isStaff() ? rank.getName() + " " : "",
                        flp.username,
                        fmessage
                    );
                } catch (TextUtils2.ParserError parserError) {
                    parserError.printStackTrace();
                }
            }
        }
    }
}
