package net.farlands.sanctuary.mechanic;

import com.kicas.rp.util.Pair;
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

import net.farlands.sanctuary.util.TimeInterval;
import net.md_5.bungee.api.chat.BaseComponent;

import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Chat extends Mechanic {
    public static final List<ChatColor> ILLEGAL_COLORS = Arrays.asList(ChatColor.MAGIC, ChatColor.BLACK);
    public static final List<Character> COLOR_CHARS = Arrays.asList('0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'l', 'm', 'n', 'o', 'r', 'x');
    private static final List<String> DISCORD_CHARS = Arrays.asList("_", "~", "*", ":", "`", "|", ">");
    private static final MessageFilter MESSAGE_FILTER = new MessageFilter();
    private final List<BaseComponent[]> rotatingMessages;
    public static Pair<Integer, Integer> itemShare = new Pair<>();
    public static Pair<Integer, Integer> taggedPlayer = new Pair<>();

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

        addRotatingMessage("&(gold)All griefing is currently off limits, even if a build is unclaimed!");

        // Wait for any dynamically added messages to be registered
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), this::scheduleRotatingMessages, 15L * 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        event.setJoinMessage(null);
        if (flp.vanished)
            Logging.broadcastStaff(ChatColor.YELLOW + event.getPlayer().getName() + " has joined silently.");
        else
            playerTransition(flp, true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        event.setQuitMessage(null);
        if (flp.vanished)
            Logging.broadcastStaff(ChatColor.YELLOW + event.getPlayer().getName() + " has left silently.");
        else
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
                    TextUtils.escapeExpression(event.getMessage())));
            return;
        }
        chat(flp, player, event.getMessage());
    }

    public static void chat(OfflineFLPlayer senderFlp, Player sender, String message) {
        Rank displayedRank = senderFlp.getDisplayRank();
        String displayPrefix;
        if (senderFlp.nickname != null && !senderFlp.nickname.isEmpty()) {
            displayPrefix = "{" + displayedRank.getColor() + "" + (displayedRank.isStaff() ? ChatColor.BOLD : "") +
                    displayedRank.getName() + " {$(hover," + senderFlp.username + ",%0%1:)}} ";
        } else {
            displayPrefix = "{" + displayedRank.getColor() + "" + (displayedRank.isStaff() ? ChatColor.BOLD : "") +
                    displayedRank.getName() + " {%0%1:}} ";
        }
        chat(senderFlp, sender, displayPrefix, message.trim());
    }

    public static void chat(OfflineFLPlayer senderFlp, Player sender, String displayPrefix, String message) {
        if (!senderFlp.rank.isStaff() && MESSAGE_FILTER.autoCensor(removeColorCodes(message))) {
            message = applyColorCodes(senderFlp.rank, message);
            // Make it seem like the message went through for the sender
            TextUtils.sendFormatted(
                    sender,
                    displayPrefix + TextUtils.escapeExpression(message),
                    senderFlp.rank.getNameColor(),
                    senderFlp.getDisplayName()
            );
            Logging.broadcastStaff(String.format(ChatColor.RED + "[AUTO-CENSOR] %s: " + ChatColor.GRAY + "%s",
                    sender.getDisplayName(), message), DiscordChannel.ALERTS);
            return;
        } else {
            message = applyColorCodes(senderFlp.rank, message);
            itemShare = new Pair<>(); taggedPlayer = new Pair<>();
            message = itemShare(senderFlp.rank, message, sender);
            message = atPlayer(message);
            message = escapeExpression(message);
        }

        if (removeColorCodes(message).length() < 1) {
            return;
        }
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
                     fmessage = displayPrefix + lmessage,
                censorMessage = displayPrefix + Chat.getMessageFilter().censor(lmessage);
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> !session.handle.isIgnoring(senderFlp))
                .forEach(session -> {
                    if (session.handle.censoring)
                        TextUtils.sendFormatted(session.player, senderFlp.rank.isStaff(), censorMessage, senderFlp.rank.getNameColor(), senderFlp.getDisplayName());
                    else
                        TextUtils.sendFormatted(session.player, senderFlp.rank.isStaff(), fmessage,      senderFlp.rank.getNameColor(), senderFlp.getDisplayName());
                });
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.IN_GAME, TextUtils.format(fmessage, senderFlp.rank.getNameColor(), senderFlp.getDisplayName()));
        // never send the nickname to console/logs
        TextUtils.sendFormatted(Bukkit.getConsoleSender(), fmessage, senderFlp.rank.getNameColor(), senderFlp.username);
    }

    public static String escapeExpression(String message) {
        if (itemShare.getFirst() != null && taggedPlayer.getFirst() == null) {
            String beginning = TextUtils.escapeExpression(message.substring(0, itemShare.getFirst()));
            String middle = message.substring(itemShare.getFirst(), itemShare.getSecond());
            String end = TextUtils.escapeExpression(message.substring(itemShare.getSecond()));
            message = beginning + middle + end;
        } else if (itemShare.getFirst() == null && taggedPlayer.getFirst() != null) {
            String beginning = TextUtils.escapeExpression(message.substring(0, taggedPlayer.getFirst()));
            String middle = message.substring(taggedPlayer.getFirst(), taggedPlayer.getSecond());
            String end = TextUtils.escapeExpression(message.substring(taggedPlayer.getSecond()));
            message = beginning + middle + end;
        } else if (itemShare.getFirst() != null && taggedPlayer.getFirst() != null) {
            String one = TextUtils.escapeExpression(message.substring(0, Math.min(taggedPlayer.getFirst(), itemShare.getFirst())));
            String two = message.substring(Math.min(taggedPlayer.getFirst(), itemShare.getFirst()),
                    Math.min(taggedPlayer.getSecond(), itemShare.getSecond()));
            String three = TextUtils.escapeExpression(message.substring(Math.min(taggedPlayer.getSecond(), itemShare.getSecond()),
                    Math.max(taggedPlayer.getFirst(), itemShare.getFirst())));
            String four = message.substring(Math.max(taggedPlayer.getFirst(), itemShare.getFirst()),
                    Math.max(taggedPlayer.getSecond(), itemShare.getSecond()));
            String five = TextUtils.escapeExpression(message.substring(Math.max(taggedPlayer.getSecond(), itemShare.getSecond())));
            message = one + two + three + four + five;
        } else {
            return TextUtils.escapeExpression(message);
        }
        return message;
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
        for (String c : DISCORD_CHARS) {
            message = message.replaceAll(String.valueOf(new char[] {'\\', c.charAt(0)}), "\\\\" + c);
        }
        message = message.replaceAll("@", "\\\\@ ");
        return removeColorCodes(message);
    }

    public static final Pattern pattern6 = Pattern.compile("&#[a-fA-F0-9]{6}");
    public static final Pattern pattern3 = Pattern.compile("&#[a-fA-F0-9]{3}");
    public static List<Integer> formatted = new CopyOnWriteArrayList<>();

    public static String removeColorCodes(String message) {
        Matcher match6 = pattern6.matcher(message);
        Matcher match3 = pattern3.matcher(message);
        while (match6.find()) {
            String color = message.substring(match6.start(), match6.end());
            message = message.replace(color, "");
            match6 = pattern6.matcher(message);
            match3 = pattern3.matcher(message);
        }
        while (match3.find()) {
            String color = message.substring(match3.start(), match3.end());
            message = message.replace(color, "");
            match3 = pattern3.matcher(message);
        }
        StringBuilder sb = new StringBuilder(message.length());
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] == '&' || chars[i] == ChatColor.COLOR_CHAR) {
                try {
                    if (COLOR_CHARS.contains(chars[i+1])) {
                        ++i;
                        continue;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    // do nothing :)
                    // allows for removing color codes in tab complete methods
                }
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    public static String applyColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String applyColorCodes(Rank rank, String message) {
        if (rank == null || rank.specialCompareTo(Rank.ADEPT) < 0)
            return removeColorCodes(message);
        message = applyColorCodes(message);
        if (!rank.isStaff()) {
            for (org.bukkit.ChatColor color : ILLEGAL_COLORS) {
                message = message.replaceAll(ChatColor.COLOR_CHAR + Character.toString(color.getChar()), "");
            }
        }
        message = sixChar(message);
        message = threeChar(message);
        formatted.clear();
        return message;
    }

    public static String sixChar(String message) {
        Matcher match6 = pattern6.matcher(message);
        while (match6.find()) {
            String color = message.substring(match6.start()+1, match6.end());
            if (!formatted.contains(match6.start())) {
                if (getLuma(color) > 16) {
                    StringBuilder sb = new StringBuilder(message);
                    sb.delete(match6.start(), match6.start() + 8);
                    sb.insert(match6.start(), net.md_5.bungee.api.ChatColor.of(color));
                    message = sb.toString();
                } else {
                    StringBuilder sb = new StringBuilder(message);
                    sb.delete(match6.start(), match6.start() + 8);
                    message = sb.toString();
                }
                formatted.add(match6.start());
                match6 = pattern6.matcher(message);
            }
        }
        return message;
    }

    public static String threeChar(String message) {
        Matcher match3 = pattern3.matcher(message);
        while (match3.find()) {
            String color = message.substring(match3.start()+1, match3.end());
            char[] chars = color.toCharArray();
            String newColor = String.valueOf(chars[0]) + chars[1] + chars[1] + chars[2] + chars[2] + chars[3] + chars[3];
            if (!formatted.contains(match3.start())) {
                if (getLuma(newColor) > 16) {
                    StringBuilder sb2 = new StringBuilder(message);
                    sb2.delete(match3.start(), match3.start() + 5);
                    sb2.insert(match3.start(), net.md_5.bungee.api.ChatColor.of(newColor));
                    message = sb2.toString();
                } else {
                    StringBuilder sb2 = new StringBuilder(message);
                    sb2.delete(match3.start(), match3.start() + 5);
                    message = sb2.toString();
                }
                formatted.add(match3.start());
                match3 = pattern3.matcher(message);
            }
        }
        return message;
    }

    // Returns luminescence of a given 6 char hex string
    public static double getLuma(String color) {
        int red = Integer.valueOf(color.substring(1,3), 16);
        int green = Integer.valueOf(color.substring(3,5), 16);
        int blue = Integer.valueOf(color.substring(5,7), 16);
        return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
    }

    public static String itemShare(Rank rank, String message, Player player) {
        if (rank == null || rank.specialCompareTo(Rank.ADEPT) < 0)
            return message;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return message;
        }

        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        net.minecraft.server.v1_16_R3.NBTTagCompound compound = new NBTTagCompound();
        compound = nmsItemStack.save(compound);
        String json = compound.toString(); // standard object

        String name;
        if (item.getItemMeta().getDisplayName().equals("")) {
            name = item.getType().name().replace("_", " ");
            name = WordUtils.capitalizeFully(name);
        } else
            name = item.getItemMeta().getDisplayName();

        message = message.replace("[item]", "[i]");
        message = message.replace("[hand]", "[i]");
        if (message.contains("[i]")) {
            String insert = "{$(item,"+json+",&(aqua)[i] " + name + "&(white)" + "" + ")}";
            message = StringUtils.replaceOnce(message, "[i]", insert);
            itemShare.setFirst(message.indexOf("{$")); itemShare.setSecond(message.indexOf("))}")+3);
        }
        return message;
    }

    public static String atPlayer(String message) {

        String playerName;
        Bukkit.getConsoleSender().sendMessage(String.valueOf(message.indexOf('@')+1));
        // Need this in case the player name comes at the very end
        try {
            playerName = message.substring(message.indexOf('@')+1, message.indexOf(" ", message.indexOf('@')));
        } catch (Exception ignored) {
            playerName = message.substring(message.indexOf('@')+1);
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(playerName);
        if (flp == null) {
            Bukkit.getConsoleSender().sendMessage("Couldn't find player: " + playerName);
            return message;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(flp.uuid);
        String name;
        if (flp.nickname == null || flp.nickname.equals(""))
            name = flp.username;
        else
            name = flp.nickname;

        String hover = "{$(hover,&(green)"+
                ChatColor.GOLD  + name + "'s" + ChatColor.GOLD + " Stats:" + ChatColor.GREEN + "\n" +
                "Rank: " + flp.rank.getColor() + flp.rank.getName() + ChatColor.GREEN + "\n" +
                "Time Played: " + TimeInterval.formatTime(flp.secondsPlayed * 1000L, false) + "\n" +
                "Last Seen: " + TimeInterval.formatTime(System.currentTimeMillis() - flp.getLastLogin(), false) + "\n" +
                "Deaths: " + offlinePlayer.getStatistic(Statistic.DEATHS) + "\n" +
                "Votes this Month: " + flp.monthVotes + "\n" +
                "Total Votes this Season: " + flp.totalSeasonVotes + "\n" +
                "Total Votes All Time: " + flp.totalVotes + ","+ flp.rank.getNameColor() + "@" + flp.username + ")}";
        message = message.replaceFirst(String.valueOf(message.charAt(message.indexOf('@'))), "").replace(playerName, hover);
        taggedPlayer.setFirst(message.indexOf("{$(h")); taggedPlayer.setSecond(message.indexOf(flp.username + ")}")+2+flp.username.length());

        return message;
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
