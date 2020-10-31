package net.farlands.sanctuary.discord;

import com.kicas.rp.util.Pair;
import com.kicas.rp.util.TextUtils;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.requests.GatewayIntent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.PluginData;
import net.farlands.sanctuary.data.struct.Proposal;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.Logging;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.kicas.rp.util.TextUtils.sendFormatted;

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
        long online = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                .count();

        return Activity.of(Activity.ActivityType.DEFAULT, "with " + online + " online player" + (online == 1 ? "" : "s"));
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
        FarLands.getScheduler().scheduleSyncRepeatingTask(this::updateStats, 0L, 1200L);
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
        String message = event.getMessage().getContentDisplay();
        if (message.startsWith("/") && FarLands.getCommandHandler().handleDiscordCommand(sender, event.getMessage()))
            return;
        message = TextUtils.escapeExpression(Chat.removeColorCodes(message));
        message = message.substring(0, Math.min(256, message.length())).trim();
        if (!event.getMessage().getAttachments().isEmpty()) {
            if (message.isEmpty())
                message = "[Image]";
            else
                message += " [Image]";
        }
        final String fmessage = translateFormatting(message);

        if (channelHandler.getChannel(DiscordChannel.STAFF_COMMANDS).getIdLong() == event.getChannel().getIdLong()) {
            if (channelHandler.getChannel(DiscordChannel.STAFF_COMMANDS).getIdLong() == event.getChannel().getIdLong()) {
                TextUtils.sendFormatted(
                        Bukkit.getConsoleSender(),
                        "&(red)[SC] %0: %1",
                        sender.getName(),
                        fmessage
                );

                FarLands.getDataHandler().getSessions().stream()
                        .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                        .forEach(session -> sendFormatted(
                                session.player,
                                "%0[SC] %1: %2",
                                session.handle.staffChatColor,
                                sender.getName(),
                                fmessage
                        ));
            }
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
                Bukkit.getOnlinePlayers().stream().filter(p -> !FarLands.getDataHandler().getOfflineFLPlayer(p).isIgnoring(flp)).forEach(p ->
                    TextUtils.sendFormatted(
                            p,
                            "&(dark_gray)DISCORD %0%1: &(white)%2",
                            rank.getNameColor(),
                            flp.username,
                            FarLands.getDataHandler().getOfflineFLPlayer(p).censoring ? censorMessage : fmessage
                    )
                );

                TextUtils.sendFormatted(
                        Bukkit.getConsoleSender(),
                        "&(dark_gray)DISCORD %0%1: &(white)%2",
                        rank.getNameColor(),
                        flp.username,
                        fmessage
                );
            }
        }
    }

    private static final int BOLD = 0x01;
    private static final int ITALIC1 = 0x02; // _italic_
    private static final int ITALIC2 = 0x04; // *italic*
    private static final int UNDERLINE = 0x08;
    private static final int STRIKETHROUGH = 0x10;

    private static String translateFormatting(String string) {
        FormatterState state = new FormatterState();
        char[] chars = string.toCharArray();
        for (;state.cursor < chars.length;++ state.cursor) {
            state.prev = state.cursor == 0 ? '\0' : chars[state.cursor - 1];
            state.current = chars[state.cursor];
            state.next = state.cursor == chars.length - 1 ? '\0' : chars[state.cursor + 1];
            state.nextNext = state.cursor >= chars.length - 2 ? '\0' : chars[state.cursor + 2];

            switch (state.current) {
                case '_': {
                    if (state.next == '_') {
                        if (applyFormat(state, UNDERLINE, "underline", Buffer.ANY, Buffer.ANY, false))
                            continue;
                    } else {
                        if (applyFormat(state, ITALIC1, "italic", Buffer.WHITESPACE, Buffer.RIGHT_WHITESPACE, true))
                            continue;
                    }
                    break;
                }

                case '*': {
                    if (state.next == '*') {
                        if (applyFormat(state, BOLD, "bold", Buffer.ANY, Buffer.ANY, false))
                            continue;
                    } else {
                        if (applyFormat(state, ITALIC2, "italic", Buffer.RIGHT_TEXT_ONLY, Buffer.LEFT_TEXT_ONLY, true))
                            continue;
                    }
                    break;
                }

                case '~': {
                    if (state.next == '~' && applyFormat(state, STRIKETHROUGH, "strikethrough", Buffer.ANY, Buffer.ANY, false))
                        continue;
                    break;
                }
            }

            state.sb.append(state.current);
        }

        return state.sb.toString();
    }

    private static boolean applyFormat(
            FormatterState state,
            int flag,
            String format,
            Buffer start,
            Buffer end,
            boolean oneChar
    ) {
        char next = oneChar ? state.next : state.nextNext;
        if (isSet(flag, state.flags)) {
            if (next == '\0' || end.test(state.prev, state.current, next)) {
                state.sb.append("&(!").append(format).append(')');
                state.flags ^= flag;
                if (oneChar)
                    state.undo.remove(flag);
                else
                    ++ state.cursor;
                return true;
            } else {
                Pair<Integer, String> undo = state.undo.remove(flag);
                if (undo != null) {
                    state.sb.insert(undo.getFirst(), undo.getSecond());
                    state.flags ^= flag;
                }
                return false;
            }
        } else {
            if (state.prev == '\0' || start.test(state.prev, state.current, next)) {
                state.sb.append("&(").append(format).append(')');
                state.flags ^= flag;
                if (oneChar)
                    state.undo.put(flag, new Pair<>(state.sb.length(), "&(!" + format + ")" + state.current));
                else
                    ++ state.cursor;
                return true;
            }
        }
        return false;
    }

    private static boolean isSet(int flag, int flags) {
        return (flags & flag) != 0;
    }

    private static class FormatterState {
        StringBuilder sb;
        int cursor, flags;
        char prev, current, next, nextNext;
        Map<Integer, Pair<Integer, String>> undo;

        FormatterState() {
            this.sb = new StringBuilder();
            this.cursor = this.flags = 0;
            this.prev = this.current = this.next = this.nextNext = '\0';
            this.undo = new HashMap<>();
        }
    }

    private enum Buffer {
        WHITESPACE,
        RIGHT_WHITESPACE,
        RIGHT_TEXT_ONLY,
        LEFT_TEXT_ONLY,
        ANY;

        boolean test(char prev, char cur, char next) {
            switch (this) {
                case RIGHT_TEXT_ONLY:
                    return Character.isLetterOrDigit(next) || cur == next;
                case LEFT_TEXT_ONLY:
                    return Character.isLetterOrDigit(prev) || cur == prev;
                case RIGHT_WHITESPACE:
                    return !Character.isWhitespace(prev) && Character.isWhitespace(next);
                case ANY:
                    return true;
            }

            return prev == '\0' || next == '\0' || Character.isWhitespace(prev) || Character.isWhitespace(next);
        }
    }
}
