package net.farlands.sanctuary.mechanic;

import com.kicas.rp.util.TextUtils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandMessage;
import net.farlands.sanctuary.command.staff.CommandStaffChat;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import net.md_5.bungee.api.chat.BaseComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Chat extends Mechanic {
    public static final List<ChatColor> ILLEGAL_COLORS = Arrays.asList(ChatColor.MAGIC, ChatColor.BLACK);
    private static final List<String> DISCORD_CHARS = Arrays.asList("_", "~", "*", ":", "`", "|", ">");
    private static final MessageFilter MESSAGE_FILTER = new MessageFilter();
    private final List<BaseComponent[]> rotatingMessages;

    Chat() {
        this.rotatingMessages = new ArrayList<>();
    }

    public void addRotatingMessage(String message) {
        rotatingMessages.add(TextUtils.format(message));
    }

    private void scheduleRotatingMessages() {
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            int messageCount = rotatingMessages.size(), rotatingMessageGap = FarLands.getFLConfig().rotatingMessageGap * 60 * 20;
            for (int i = 0; i < messageCount; ++i) {
                BaseComponent[] message = rotatingMessages.get(i);
                Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), () -> Bukkit.getOnlinePlayers().forEach(player ->
                        player.spigot().sendMessage(message)), i * rotatingMessageGap + 600, messageCount * rotatingMessageGap);
            }
        }, 5L);
    }

    @Override
    public void onStartup() {
        FarLands.getFLConfig().rotatingMessages.stream().map(TextUtils::format).forEach(rotatingMessages::add);
        // Wait for any dynamically added messages to be registered
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), this::scheduleRotatingMessages, 15L * 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        event.setJoinMessage(null);
        if (flp.vanished)
            Logging.broadcastStaff(ChatColor.YELLOW + event.getPlayer().getName() + " joined silently.");
        else
            playerTransition(flp, true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        event.setQuitMessage(null);
        if (!flp.vanished)
            playerTransition(flp, false);
    }

    public static void playerTransition(OfflineFLPlayer flp, boolean joinMessage) {
        String joinOrLeave = joinMessage ? "joined." : "left.";
        Bukkit.getOnlinePlayers().forEach(player -> TextUtils.sendFormatted(player, "{&(yellow,bold) > }" +
                flp.rank.getNameColor() + flp.username + "&(yellow) has " + joinOrLeave));
        TextUtils.sendFormatted(Bukkit.getConsoleSender(), "{&(yellow,bold) > }" +
                flp.rank.getNameColor() + flp.username + "&(yellow) has " + joinOrLeave);
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, "> " + flp.username + " has " + joinOrLeave);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, event.getDeathMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    // Process this more superficial, non-critical stuff last
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);

        spamUpdate(player, event.getMessage());
        if (flp.isMuted()) {
            flp.currentMute.sendMuteMessage(player);
            Logging.broadcastStaff(TextUtils.format("&(red)[MUTED] %0: &(gray)%1", event.getPlayer().getName(),
                    event.getMessage()));
            return;
        }
        chat(flp, player, event.getMessage());
    }

    public static void chat(OfflineFLPlayer senderFlp, Player sender, String message) {
        Rank rank = senderFlp.rank,
                displayedRank = senderFlp.topVoter && !rank.isStaff() ? Rank.VOTER : rank;
        String displayPrefix;
        if (senderFlp.nickname != null && !senderFlp.nickname.isEmpty()) {
            displayPrefix = "{" + displayedRank.getColor() + "" + (displayedRank.isStaff() ? ChatColor.BOLD : "") +
                    displayedRank.getName() + " {$(hover," + senderFlp.username + ",%0%1)}:} ";
        } else {
            displayPrefix = "{" + displayedRank.getColor() + "" + (displayedRank.isStaff() ? ChatColor.BOLD : "") +
                    displayedRank.getName() + " {%0%1}:} ";
        }
        chat(senderFlp, sender, displayPrefix, message.trim());
    }

    public static void chat(OfflineFLPlayer senderFlp, Player sender, String displayPrefix, String message) {
        if (!senderFlp.rank.isStaff() && MESSAGE_FILTER.autoCensor(removeColorCodes(message))) {
            message = applyColorCodes(senderFlp.rank, message);
            // Make it seem like the message went through for the sender
            TextUtils.sendFormatted(sender, displayPrefix + TextUtils.escapeExpression(message), senderFlp.rank.getColor(), senderFlp.getDisplayName());
            Logging.broadcastStaff(String.format(ChatColor.RED + "[AUTO-CENSOR] %s: " + ChatColor.GRAY + "%s",
                    sender.getDisplayName(), message), DiscordChannel.ALERTS);
            return;
        } else
            message = applyColorCodes(senderFlp.rank, message);


        if (message.startsWith("!")) {
            if (message.length() <= 1)
                return;
            message = message.substring(1);
        } else {
            FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
            if (session.autoSendStaffChat) {
                FarLands.getCommandHandler().getCommand(CommandStaffChat.class).execute(sender, new String[]{"c", message});
                return;
            }
            if (session.replyToggleRecipient != null) {
                if (session.replyToggleRecipient instanceof Player && ((Player) session.replyToggleRecipient).isOnline()) {
                    CommandMessage.sendMessages(session.replyToggleRecipient, sender, message);
                    return;
                } else {
                    sender.sendMessage(ChatColor.RED + session.replyToggleRecipient.getName() +
                            " is no longer online, your reply toggle has been turned off.");
                    session.replyToggleRecipient = null;
                }
            }
        }
        final String lmessage = limitCaps(limitFlood(message)),
                     fmessage = displayPrefix + TextUtils.escapeExpression(lmessage),
                censorMessage = displayPrefix + TextUtils.escapeExpression(Chat.getMessageFilter().censor(lmessage));
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> !session.handle.isIgnoring(senderFlp))
                .forEach(session -> {
                    if (session.handle.censoring)
                        TextUtils.sendFormatted(session.player, censorMessage, senderFlp.rank.getColor(), senderFlp.getDisplayName());
                    else
                        TextUtils.sendFormatted(session.player, fmessage,      senderFlp.rank.getColor(), senderFlp.getDisplayName());
                });
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, TextUtils.format(fmessage, senderFlp.rank.getColor(), senderFlp.getDisplayName()));
        // never send the nickname to console/logs
        TextUtils.sendFormatted(Bukkit.getConsoleSender(), fmessage, senderFlp.rank.getColor(), senderFlp.username);
    }

    public void spamUpdate(Player player, String message) {
        if (Rank.getRank(player).isStaff())
            return;
        FLPlayerSession session = FarLands.getDataHandler().getSession(player);
        double strikes = session.spamAccumulation;
        if (FLUtils.deltaEquals(strikes, 0.0, 1e-8))
            session.spamCooldown.reset(() -> session.spamAccumulation = 0.0);
        strikes += 1 + message.length() / 80.0;
        if (strikes >= 7.0) {
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                player.kickPlayer("Kicked for spam. Repeating this offense could result in a ban.");
                AntiCheat.broadcast(player.getName() + " was kicked for spam.", true);
            });
        }
    }

    public static String limitFlood(String message) {
        int row = 0;
        char last = ' ';
        StringBuilder output = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (Character.toLowerCase(c) == last) {
                if (++row < 4)
                    output.append(c);
            } else {
                last = Character.toLowerCase(c);
                output.append(c);
                row = 0;
            }
        }
        return output.toString();
    }

    public static String limitCaps(String message) {
        if (message.length() < 6)
            return message;
        float uppers = 0;
        for (char c : message.toCharArray()) {
            if (Character.isUpperCase(c))
                ++uppers;
        }
        if (uppers / message.length() >= 5f / 12)
            return message.substring(0, 1).toUpperCase() + message.substring(1).toLowerCase();
        else
            return message;
    }

    public static MessageFilter getMessageFilter() {
        return MESSAGE_FILTER;
    }

    public static String applyDiscordFilters(String message) {
        message = message.replaceAll("\\\\", "\\\\\\\\");
        for (String c : DISCORD_CHARS)
            message = message.replaceAll("\\" + c, "\\\\" + c);
        message = message.replaceAll("@", "\\\\@ ");
        return removeColorCodes(message);
    }

    public static String removeColorCodes(String message) {
        StringBuilder sb = new StringBuilder(message.length());
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] == '&' || chars[i] == ChatColor.COLOR_CHAR) {
                ++i;
                continue;
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    public static String applyColorCodes(Rank rank, String message) {
        if (rank == null || rank.specialCompareTo(Rank.ADEPT) < 0)
            return removeColorCodes(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (!rank.isStaff()) {
            for (ChatColor color : ILLEGAL_COLORS)
                message = message.replaceAll(ChatColor.COLOR_CHAR + Character.toString(color.getChar()), "");
        }
        return message;
    }

    public static String applyColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static class MessageFilter {
        private final Map<String, Boolean> words;
        private final List<String> replacements;
        private final Random rng;

        private void init() {
            try {
                Stream.of(FarLands.getDataHandler().getDataTextFile("censor-dict.txt").split("\n")).forEach(line -> {
                    boolean ac = line.startsWith("AC:");
                    words.put(ac ? line.substring(3) : line, ac);
                });
                replacements.addAll(Arrays.asList(FarLands.getDataHandler().getDataTextFile("censor-replacements.txt").split("\n")));
            } catch (IOException ex) {
                Logging.error("Failed to load words and replacements for message filter words.");
                throw new RuntimeException(ex);
            }
        }

        MessageFilter() {
            this.words = new HashMap<>();
            this.replacements = new ArrayList<>();
            this.rng = new Random();
            init();
        }

        public String censor(String s) {
            String censored = s.toLowerCase();
            for (String word : words.keySet())
                censored = censored.replaceAll("(^|\\W)\\Q" + word + "\\E($|\\W)", getRandomReplacement());
            return FLUtils.matchCase(s, censored);
        }

        public boolean isProfane(String s) {
            return !s.equals(censor(s));
        }

        public boolean autoCensor(String s) {
            String censored = s.toLowerCase();
            for (String word : words.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList()))
                censored = censored.replaceAll("(^|\\W)\\Q" + word + "\\E($|\\W)", " ");
            return !s.equalsIgnoreCase(censored);
        }

        String getRandomReplacement() {
            return ' ' + replacements.get(rng.nextInt(replacements.size())) + ' ';
        }
    }
}
