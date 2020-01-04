package net.farlands.odyssey.discord;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.PluginData;
import net.farlands.odyssey.data.struct.Proposal;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TimeInterval;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Stream;

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
        config = FarLands.getFLConfig().getDiscordBotConfig();
        try {
            if (config.getToken().isEmpty()) {
                Chat.log("The bot token was not set. Discord integration will not operate.");
                return;
            }
            if (config.getServerID() == 0L) {
                Chat.log("The serverID was not set. Discord integration will not operate.");
                return;
            }
            jdaBot = (new JDABuilder(AccountType.BOT)).setToken(config.getToken()).addEventListener(this)
                    .setAutoReconnect(true).setGame(Game.of(Game.GameType.DEFAULT, "with " +
                            FarLands.getDataHandler().getOfflineFLPlayers().size() + " player records"))
                    .setStatus(OnlineStatus.ONLINE).buildAsync();
        } catch (Exception ex) {
            Chat.error("Failed to setup discord jdaBot.");
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
        return jdaBot.getGuildById(config.getServerID());
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
        jdaBot.getPresence().setGame(Game.of(Game.GameType.DEFAULT, "with " +
                FarLands.getDataHandler().getOfflineFLPlayers().size() + " player records"));
        int online = (int) Bukkit.getOnlinePlayers().stream()
                .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished).count();
        long restartTime = FarLands.getFLConfig().getRestartTime();
        (new ChannelManager((TextChannel) channelHandler.getChannel("ingame"))).setTopic(online + " player" + (online == 1 ? "" : "s") +
                " online. Next restart in " + TimeInterval.formatTime(86459999L - ((System.currentTimeMillis() - restartTime) % 86400000L),
                false, TimeInterval.MINUTE)).queue();
    }

    public void sendMessage(MessageChannel channel, String message) {
        channelHandler.sendMessage(channel, message);
    }

    public void sendMessageRaw(String channelName, String message) {
        if (!active)
            return;
        channelHandler.sendMessage(channelName, message);
    }

    public void sendMessage(String channelName, String message) {
        sendMessageRaw(channelName, Chat.applyDiscordFilters(message));
    }

    public void sendMessage(String channelName, BaseComponent[] message) {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent bc : message) {
            if (bc instanceof TextComponent)
                sb.append(((TextComponent) bc).getText());
        }
        sendMessage(channelName, sb.toString());
    }

    public MessageChannel getChannel(String key) {
        return channelHandler.getChannel(key);
    }

    public static boolean isManagedRole(Role role) {
        return Stream.of(Rank.VALUES).anyMatch(rank -> role.getName().equals(rank.getSymbol())) ||
                STAFF_ROLE.equals(role.getName()) || VERIFIED_ROLE.equals(role.getName());
    }

    @Override
    public void onReady(ReadyEvent event) {
        config.getChannels().forEach((name, id) -> channelHandler.setChannel(name, id == 0L ? null : jdaBot.getTextChannelById(id)));
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
        DiscordSender sender = new DiscordSender(event.getAuthor(), event.getChannel());
        String message = event.getMessage().getContentRaw();
        if (message.startsWith("/") && FarLands.getCommandHandler().handleDiscordCommand(sender, event.getMessage()))
            return;
        message = Chat.removeColorCodes(message.replaceAll("\\s+", " "));
        final String fmessage = message.substring(0, Math.min(256, message.length()));
        if (channelHandler.getChannel("staffcommands").getIdLong() == event.getChannel().getIdLong())
            Chat.broadcastStaff(ChatColor.RED + "[SC] " + sender.getName() + ": " + fmessage);
        else if (channelHandler.getChannel("ingame").getIdLong() == event.getChannel().getIdLong()) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            if (flp != null) {
                if (flp.isMuted() || flp.isBanned()) {
                    sender.getUser().openPrivateChannel().queue(channel ->
                            channel.sendMessage("You cannot send messages through in-game chat while muted or banned.").queue()
                    );
                    event.getMessage().delete().queue();
                    return;
                }

                Rank rank = flp.getRank();

                if (!rank.isStaff() && Chat.getMessageFilter().autoCensor(fmessage)) {
                    sendMessageRaw("alerts", "Deleted message from in-game channel:\n```" + fmessage +
                            "```\nSent by: `" + sender.getName() + "`.");
                    event.getMessage().delete().queue();
                    return;
                }

                Bukkit.getOnlinePlayers().stream().filter(p -> !FarLands.getDataHandler().getOfflineFLPlayer(p).isIgnoring(flp)).forEach(p -> {
                    boolean censor = FarLands.getDataHandler().getOfflineFLPlayer(p).isCensoring();
                    p.sendMessage(String.format(ChatColor.DARK_GRAY + "DISCORD" +
                            (rank.specialCompareTo(Rank.DONOR) >= 0 ? rank.getColor().toString() : "") + " %s: " +
                            ChatColor.WHITE + "%s", flp.getUsername(), censor ? Chat.getMessageFilter().censor(fmessage) : fmessage));
                });
                Bukkit.getConsoleSender().sendMessage(String.format(ChatColor.DARK_GRAY + "DISCORD" +
                        (rank.specialCompareTo(Rank.DONOR) >= 0 ? rank.getColor().toString() : "") + " %s: " +
                        ChatColor.WHITE + "%s", flp.getUsername(), fmessage));
            }
        }
    }
}
