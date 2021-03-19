package net.farlands.sanctuary.mechanic;

import com.kicas.rp.util.TextUtils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandMessage;
import net.farlands.sanctuary.command.player.CommandShrug;
import net.farlands.sanctuary.command.player.CommandStats;
import net.farlands.sanctuary.command.staff.CommandStaffChat;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.markdown.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import net.md_5.bungee.api.chat.BaseComponent;

import net.minecraft.server.v1_16_R3.NBTTagCompound;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;
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
            Logging.broadcastStaff(
                    ChatColor.YELLOW + event.getPlayer().getName() + " has joined silently.",
                    DiscordChannel.STAFF_COMMANDS
            );
        else
            playerTransition(flp, true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        event.setQuitMessage(null);
        if (flp.vanished)
            Logging.broadcastStaff(
                    ChatColor.YELLOW + event.getPlayer().getName() + " has left silently.",
                    DiscordChannel.STAFF_COMMANDS
            );
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
        Player sender = event.getPlayer();
        OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        spamUpdate(sender, event.getMessage());
        if (senderFlp.isMuted()) {
            senderFlp.currentMute.sendMuteMessage(sender);
            Logging.broadcastStaff(TextUtils.format("&(red)[MUTED] %0: &(gray)%1", event.getPlayer().getName(),
                    TextUtils.escapeExpression(event.getMessage())));
            return;
        }

        chat(senderFlp, sender, event.getMessage());
    }

    public static void chat(OfflineFLPlayer senderFlp, Player sender, String message) {
        Rank displayedRank = senderFlp.getDisplayRank();
        String playerStats = CommandStats.formatStats(CommandStats.playerInfoMap(senderFlp, false), senderFlp);

        String displayPrefix = "{" + displayedRank.getColor() + "" + (displayedRank.isStaff() ? ChatColor.BOLD : "") + displayedRank.getName() +
                " {$(hover," + playerStats + "," + "%0%1:)}} ";
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
        }

        // Vanished players shouldn't be shouting
        boolean shout = message.startsWith("!") && !senderFlp.vanished;
        if (shout) {
            if (message.length() <= 1)
                return;

            message = message.substring(1);
        }

        // Do not change the order of these
        message = applyEmotes(message);
        message = applyColorCodes(senderFlp.rank, message);
        message = TextUtils.escapeExpression(message);
        message = formUrls(message);
        message = atPlayer(message, sender.getUniqueId());
        message = itemShare(senderFlp.rank, message, sender);

        if (removeColorCodes(message).length() < 1)
            return;

        if (!shout) {
            if (senderFlp.vanished) {
                if (senderFlp.rank.isStaff()) {
                    FarLands.getCommandHandler().getCommand(CommandStaffChat.class).execute(sender, new String[]{"c", message});
                    return;
                }
                sender.sendMessage(ChatColor.RED + "You are vanished, you cannot chat in-game.");
                return;
            }

            FLPlayerSession session = senderFlp.getSession();
            if (session.autoSendStaffChat) {
                FarLands.getCommandHandler().getCommand(CommandStaffChat.class).execute(sender, new String[]{"c", message});
                return;
            }

            if (session.replyToggleRecipient != null) {
                if (session.replyToggleRecipient instanceof Player && ((Player) session.replyToggleRecipient).isOnline()) {
                    CommandMessage.sendMessages(session.replyToggleRecipient, sender, message);
                    return;
                }

                sender.sendMessage(
                        ChatColor.RED + session.replyToggleRecipient.getName() +
                                " is no longer online, your reply toggle has been turned off."
                );
                session.replyToggleRecipient = null;
            }
        }
        final String lmessage = limitCaps(limitFlood(message)),
                     fmessage = displayPrefix + lmessage,
                censorMessage = displayPrefix + Chat.getMessageFilter().censor(lmessage);
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> !session.handle.getIgnoreStatus(senderFlp).includesChat())
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
        message = message.replaceAll("@", "@\u200B");
        int firstColon = Math.max(message.indexOf(':'), 0);
        return removeColorCodes(MarkdownProcessor.mcToMarkdown(message, firstColon));
    }

    public static String removeColorCodes(String message) {
        // Colorize it first to properly strip hex codes
        message = colorize(message);
        StringBuilder sb = new StringBuilder(message.length());
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] == '&' || chars[i] == ChatColor.COLOR_CHAR &&
                    i < chars.length - 1 && COLOR_CHARS.contains(chars[i + 1])) {
                ++i;
                continue;
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
        message = colorize(message);
        return message;
    }

    // Pattern matching "nicer" legacy hex chat color codes - &#rrggbb
    private static final Pattern HEX_COLOR_PATTERN_SIX = Pattern.compile("&#([0-9a-fA-F]{6})");
    // Pattern matching funny's need for 3 char hex
    private static final Pattern HEX_COLOR_PATTERN_THREE = Pattern.compile("&#([0-9a-fA-F]{3})");

    private static String colorize(String string) {
        // Do 6 char first since the 3 char pattern will also match 6 char occurrences
        StringBuffer sb6 = new StringBuffer();
        Matcher matcher6 = HEX_COLOR_PATTERN_SIX.matcher(string);
        while (matcher6.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher6.group(1).toCharArray())
                replacement.append('&').append(character);
            if (getLuma(replacement.toString()) > 16)
                matcher6.appendReplacement(sb6, replacement.toString());
            else
                matcher6.appendReplacement(sb6, "");
        }
        matcher6.appendTail(sb6);
        string = sb6.toString();

        // Now convert 3 char to the same format ex. &#363 -> &x&3&3&6&6&3&3
        StringBuffer sb3 = new StringBuffer();
        Matcher matcher3 = HEX_COLOR_PATTERN_THREE.matcher(string);
        while (matcher3.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher3.group(1).toCharArray())
                replacement.append('&').append(character).append('&').append(character);
            if (getLuma(replacement.toString()) > 16)
                matcher3.appendReplacement(sb3, replacement.toString());
            else
                matcher3.appendReplacement(sb3, "");
        }
        matcher3.appendTail(sb3);

        // Translate '&' to '§'
        return ChatColor.translateAlternateColorCodes('&', sb3.toString());
    }

    // Get luminescence passing through Bungee's hex format - &x&r&r&g&g&b&b
    public static double getLuma(String color) {
        color = color.replace("&", "").replace("x", "");
        int red   = Integer.valueOf(color.substring(0,2), 16);
        int green = Integer.valueOf(color.substring(2,4), 16);
        int blue  = Integer.valueOf(color.substring(4,6), 16);
        return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
    }

    public static String itemShare(Rank rank, String message, Player player) {
        if (rank == null || rank.specialCompareTo(Rank.ADEPT) < 0)
            return message;

        ItemStack item = player.getInventory().getItemInMainHand().clone();
        if (item.getType() == Material.AIR) {
            return message;
        }

        String name;
        if (item.getItemMeta().getDisplayName().equals("")) {
            name = item.getType().name().replace("_", " ");
            name = WordUtils.capitalizeFully(name);
        } else
            name = item.getItemMeta().getDisplayName();

        // Make TextUtils happy
        String saveName = name;
        name = name.replace("{", "").replace("}", "").replace("(", "")
                .replace(")", "").replace("$", "").replace("%", "");
        // Change the item display name to remove TextUtils characters, we'll put them back later
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Chat.colorize(name));
        item.setItemMeta(meta);

        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        net.minecraft.server.v1_16_R3.NBTTagCompound compound = new NBTTagCompound();
        compound = nmsItemStack.save(compound);
        String json = compound.toString(); // standard object

        message = message.replace("[item]", "[i]");
        message = message.replace("[hand]", "[i]");
        message = message.replace("[i]", "{$(item,"+json+",&(aqua)[i] " + name + "&(white)" +  ")}");

        // Put the item name back
        meta.setDisplayName(Chat.colorize(saveName));
        item.setItemMeta(meta);
        return message;
    }

    public static String atPlayer(String message, UUID player, boolean silent) {
        StringBuilder newMessage = new StringBuilder();
        for (String word : message.replace("{", "").replace("}", "").split(" ")) {
            if (word.startsWith("@")) {
                String name = word.substring(1).replaceAll("[^a-zA-Z]", "").toLowerCase();
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(name);
                if (flp == null) {
                    newMessage.append(word).append(" ");
                    continue;
                }

                if (
                        !silent &&
                        flp.getOnlinePlayer() != null &&
                        flp.getOnlinePlayer().isOnline() &&
                        !flp.getIgnoreStatus(FarLands.getDataHandler().getOfflineFLPlayer(player)).includesChat()
                ) {
                    flp.getOnlinePlayer().playSound(flp.getOnlinePlayer().getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
                }

                String hover = "{$(hover,&(green)" + CommandStats.formatStats(CommandStats.playerInfoMap(flp, false), flp) + "," + flp.rank.getNameColor() + "@" + flp.username + ")}";
                newMessage.append(hover).append(word.substring(name.length() + 1)).append(" ");
            } else
                newMessage.append(word).append(" ");
        }
        return "{" + newMessage.toString() + "}";
    }

    public static String atPlayer(String message, UUID player) {
        return atPlayer(message, player, false);
    }

    /**
     * Converts text links in messages into clickable links for players to click on
     * @param message The text to convert
     * @return The converted text
     */
    public static String formUrls(String message) {
        return message.replaceAll(
                "(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])",
                "\\$(hoverlink,$1,&(aqua)Follow Link,&(aqua)$1)"
        );
    }

    public static String applyEmotes(String message) {
        for (CommandShrug.TextEmote emote : CommandShrug.TextEmote.values) {
            StringBuilder sb = new StringBuilder();
            for (String word : message.split(" ")) {
                String search = ":" + emote.name().toLowerCase() + ":";
                if (word.contains(search)) {
                    if (word.contains("\\" + search)) {
                        word = word.replace("\\", "");
                    } else {
                        word = word.substring(0, word.indexOf(search)) + emote.getValue() + word
                                .substring(word.indexOf(search) + search.length());
                    }
                }
                sb.append(word).append(" ");
            }
            message = sb.toString();
        }
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
