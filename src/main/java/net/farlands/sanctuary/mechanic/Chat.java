package net.farlands.sanctuary.mechanic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kicas.rp.util.TextUtils;
import com.kicas.rp.util.TextUtils2;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.player.CommandMessage;
import net.farlands.sanctuary.command.player.CommandShrug;
import net.farlands.sanctuary.command.player.CommandStats;
import net.farlands.sanctuary.command.staff.CommandStaffChat;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.IgnoreStatus;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.markdown.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
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
import java.util.regex.MatchResult;
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

    // Keep as old formatting so that rotatingMessages don't need to be rewritten
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
        FarLands.getFLConfig().rotatingMessages.forEach(this::addRotatingMessage);

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
        try {
            BaseComponent[] msg = TextUtils2.format(
                "{&(yellow,bold) > }&(%0)%1 &(yellow)has %2",
                flp.rank.getNameColor().getName(),
                flp.username,
                joinOrLeave
            );

            Bukkit.getOnlinePlayers().forEach(player -> player.spigot().sendMessage(msg));
            Bukkit.getConsoleSender().spigot().sendMessage(msg);
        } catch (TextUtils2.ParserError e) {
            e.printStackTrace();
        }
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
            try {
                Logging.broadcastStaff(
                    TextUtils2.format(
                        "&(red)[MUTED] %0: &(gray)%1", event.getPlayer().getName(),
                        TextUtils2.escapeExpression(event.getMessage())
                    )
                );
            } catch (TextUtils2.ParserError parserError) {
                parserError.printStackTrace();
            }
            return;
        }

        chat(senderFlp, sender, event.getMessage().trim());
    }

    public static void chat(OfflineFLPlayer senderFlp, Player sender, String message) {
        chat(senderFlp, sender, generatePrefix(senderFlp), message);
    }

    public static String generatePrefix(OfflineFLPlayer senderFlp) {
        Rank displayedRank = senderFlp.getDisplayRank();
        String playerStats = CommandStats.getFormattedStats(senderFlp, false).replaceAll("([()])", "\\\\$1");
        String rank = "&(" + displayedRank.getColor().getName() + ")" + (displayedRank.isStaff() ? ChatColor.BOLD : "") + displayedRank.getName();
        return "{" + rank + " {$(click:suggest_command,/msg " + senderFlp.username + " )$(hover:show_text," + playerStats + ")&(%0)%1:}} ";
//        chat(senderFlp, sender, displayPrefix, message.trim());
    }

    public static void autoCensorAlert(Player sender, String formattedMessage, String rawMessage, OfflineFLPlayer flp) {
        boolean fakeMsg = flp.secondsPlayed < 60 * 15; // Played for more than 15 minutes
        if (fakeMsg) {
            // Make it seem like the message went through for the sender
            try {
                TextUtils2.sendFormatted(
                    sender,
                    formattedMessage,
                    flp.rank.getNameColor(),
                    flp.getDisplayName()
                );
            } catch (TextUtils2.ParserError e) {
                e.printStackTrace();
            }
        } else {
            // Let the sender know that their message wasn't sent
            try {
                TextUtils2.sendFormatted(
                    sender,
                    "&(red)You message was not sent as it may have contained words or phrases that may be offensive to some."
                );
            } catch (TextUtils2.ParserError e) {
                e.printStackTrace();
            }
        }
        Logging.broadcastStaff(
            String.format(
                ChatColor.RED + "[AUTO-CENSOR] %s: " + ChatColor.GRAY + "%s - " + ChatColor.RED + "%s",
                sender.getDisplayName(),
                rawMessage,
                fakeMsg ? "False message sent to player." : "Notified player."
            ),
            DiscordChannel.ALERTS
        );
    }

    public static void chat(OfflineFLPlayer senderFlp, Player sender, String displayPrefix, String message) {
        if (!senderFlp.rank.isStaff() && MESSAGE_FILTER.autoCensor(removeColorCodes(message))) {
            message = applyColorCodes(senderFlp.rank, message);

            // Handle Auto Censor
            autoCensorAlert(
                sender,
                displayPrefix + TextUtils2.escapeExpression(message),
                message,
                senderFlp
            );

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
        message = TextUtils2.escapeExpression(message);
        message = applyColorCodes(senderFlp.rank, message, true);
        message = formUrls(message);
        message = formCommandSuggestions(message);
        message = atPlayer(message, sender.getUniqueId());
        message = itemShare(senderFlp.rank, message, sender);


        if (removeColorCodes(message).length() < 1 || removeColorCodes(message).equals("{}"))
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

        String lmessage = limitCaps(limitFlood(message));
        String fmessage = displayPrefix + lmessage;
        String censorMessage = displayPrefix + Chat.getMessageFilter().censor(lmessage);
        String name = convertChatColorHex(senderFlp.getDisplayName());

        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> !session.handle.getIgnoreStatus(senderFlp).includesChat())
                .forEach(session -> {
                    try {
                        if (session.handle.censoring) {
                            TextUtils2.sendFormatted(session.player, censorMessage, senderFlp.rank.getNameColor().getName(), name);
                        } else {
                            TextUtils2.sendFormatted(session.player, fmessage, senderFlp.rank.getNameColor().getName(), name);
                        }
                    } catch (TextUtils2.ParserError e) {
                        e.printStackTrace();
                    }
                });
        try {
            FarLands.getDiscordHandler().sendIngameChatMessage(
                TextUtils2.format(fmessage, senderFlp.rank.getNameColor().getName(), ChatColor.stripColor(senderFlp.getDisplayName())),
                senderFlp.rank.getName().length() + ChatColor.stripColor(senderFlp.getDisplayName()).length() + 1
            );
        } catch (TextUtils2.ParserError e) {
            e.printStackTrace();
        }

        try {
            // never send the nickname to console/logs
            TextUtils2.sendFormatted(Bukkit.getConsoleSender(), fmessage, senderFlp.rank.getNameColor().getName(), senderFlp.username);
        } catch (TextUtils2.ParserError parserError) {
            parserError.printStackTrace();
        }
    }

    /**
     * Converts from ChatColor hex (§-§-§-§-§-§-) to TextUtils hex (&(#------))
     * @param chatColorHex The string to convert
     * @return The converted value
     */
    public static String convertChatColorHex(String chatColorHex) {
        return Pattern.compile("(?i)(§x(§[a-f0-9]){6})")
            .matcher(chatColorHex)
            .replaceAll(
                s ->
                    "&(#" + s.group(1).replaceAll(ChatColor.COLOR_CHAR + "x?", "") + ")"
            );
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

    public static String applyDiscordFilters(String message, int start) {
        for (String c : DISCORD_CHARS) {
            message = message.replaceAll(String.valueOf(new char[] {'\\', c.charAt(0)}), "\\\\" + c);
        }
        message = message.replaceAll("@", "@\u200B");
        return removeColorCodes(MarkdownProcessor.mcToMarkdown(message, start));
    }

    public static String applyDiscordFilters(String message){
        return applyDiscordFilters(message, 0);
    }

    public static String removeColorCodes(String message) {
        // RegEx ftw
        return message.replaceAll(
            "(?i)([&" + ChatColor.COLOR_CHAR + "][0-9a-fk-orx])|" + // Match &0 ... &f, &k ... &o, &r, and &x, Ignore Case
                "(&#[0-9a-f]{3,6})", // Match &#rrggbb and &#rgb, Ignore Case
            ""
        );
    }

    public static String applyColorCodes(Rank rank, String message, boolean useTextUtils) {
        if (rank == null || rank.specialCompareTo(Rank.ADEPT) < 0)
            return removeColorCodes(message);

        if (useTextUtils) { // Should use text utils formatting?
            message = message.replaceAll("\\\\?&\\\\?#([A-Fa-f0-9]{6})", "&(#$1)"); // &#rrggbb
            message = message.replaceAll("\\\\?&\\\\?#([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])", "&(#$1$1$2$2$3$3)"); // &#rgb
        } else {
            message = colorize(message);
        }

        message = ChatColor.translateAlternateColorCodes('&', message);

        if (!rank.isStaff()) {
            for (org.bukkit.ChatColor color : ILLEGAL_COLORS) {
                message = message.replaceAll(ChatColor.COLOR_CHAR + Character.toString(color.getChar()), "");
            }
        }
//        message = colorize(message);
        return message;
    }

    public static String applyColorCodes(Rank rank, String message) {
        return applyColorCodes(rank, message, false);
    }

    // Pattern matching "nicer" legacy hex chat color codes - &#rrggbb
    private static final Pattern HEX_COLOR_PATTERN_SIX = Pattern.compile("&#([0-9a-fA-F]{6})");
    // Pattern matching funny's need for 3 char hex
    private static final Pattern HEX_COLOR_PATTERN_THREE = Pattern.compile("&#([0-9a-fA-F]{3})");

    public static String colorize(String string) {
        // Do 6 char first since the 3 char pattern will also match 6 char occurrences
        StringBuffer sb6 = new StringBuffer();
        Matcher matcher6 = HEX_COLOR_PATTERN_SIX.matcher(string);
        while (matcher6.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher6.group(1).toCharArray())
                replacement.append('&').append(character);
            if (getLuma(replacement.toString(), false) > 16)
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
            if (getLuma(replacement.toString(), false) > 16)
                matcher3.appendReplacement(sb3, replacement.toString());
            else
                matcher3.appendReplacement(sb3, "");
        }
        matcher3.appendTail(sb3);

        // Translate '&' to '§'
        return ChatColor.translateAlternateColorCodes('&', sb3.toString());
    }

    // Get luminescence passing through Bungee's hex format - &x&r&r&g&g&b&b
    public static double getLuma(String color, boolean hex) {
        int r, g, b;
        color = color.replaceAll("[&x#]", "");

        r = Integer.valueOf(color.substring(0, 2), 16);
        g = Integer.valueOf(color.substring(2, 4), 16);
        b = Integer.valueOf(color.substring(4, 6), 16);
        return (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
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
            name = ChatColor.RESET + WordUtils.capitalizeFully(name);
        } else
            name = item.getItemMeta().getDisplayName();

        ItemStack itemClone = item.clone();
        // Change the item display name to have the correct tag values
        ItemMeta meta = itemClone.getItemMeta();
        meta.setDisplayName(Chat.colorize(name));
        itemClone.setItemMeta(meta);

        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemClone);
        net.minecraft.nbt.NBTTagCompound compound = new NBTTagCompound();
        compound = nmsItemStack.save(compound);

        JsonObject jsonObj = new JsonParser().parse(compound.toString()).getAsJsonObject();

        String tag = jsonObj.remove("tag").toString();
        jsonObj.addProperty("tag", tag);

        String countStr = jsonObj.remove("Count").getAsString();
        int count = Integer.parseInt(countStr.replace("b", ""));
        jsonObj.addProperty("count", count);

        String json = jsonObj.toString();

        message = message.replace("[item]", "[i]");
        message = message.replace("[hand]", "[i]");
        message = message.replace("[i]", "{$(hover:show_item," + json + ")&(aqua)[i] " + TextUtils2.escapeExpression(name) + "}");
        return message;
    }

    public static String atPlayer(String message, UUID player) {
        return atPlayer(message, player, false);
    }

    private static final Pattern INGAME_MENTION = Pattern.compile("(?i)(?<!\\\\)@(\\w{4,})");
    private static final Pattern DISCORD_MENTION = Pattern.compile("(?i)<@!?(\\d+)>");

    public static String atPlayer(String message, UUID sender, boolean silent) {
        List<OfflineFLPlayer> mentioned = new ArrayList<>();
        message = INGAME_MENTION.matcher(message).replaceAll(
            (matchResult) -> {
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(matchResult.group(1));
                if(flp == null) return "@$1";
                mentioned.add(flp);
                return playerMention(flp);
            }
        );
        message = DISCORD_MENTION.matcher(message).replaceAll(
            (matchResult) -> {
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(Long.parseLong(matchResult.group(1)));
                if(flp == null) return "@$1"; // If an unverified player is mentioned - This should almost never happen.
                mentioned.add(flp);
                return playerMention(flp);
            }
        );

        // Play sound for mentioned players, if not ignored and they're online
        mentioned.forEach(flp -> {
            if(!silent && !flp.ignoreStatusMap.getOrDefault(sender, IgnoreStatus.NONE).includesChat() && flp.getOnlinePlayer() != null) {
                flp.getOnlinePlayer().playSound(flp.getOnlinePlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 6.0F, 1.0F);
            }
        });
        return message;
    }

    private static String playerMention(OfflineFLPlayer flp) {
        String messageCommand = flp.getOnlinePlayer() == null ? "/mail send " : "/msg ";
        String hoverText = CommandStats.getFormattedStats(flp, false, false).replaceAll("([()])", "\\\\$1");
        return
            "{" +
                "\\$(click:suggest_command," + messageCommand + flp.username + " )" +
                "\\$(hover:show_text," + hoverText + ")" +
                "&(" + flp.getDisplayRank().getNameColor().getName() + ")@" + flp.username +
            "}";
    }

    /**
     * Converts text links in messages into clickable links for players to click on
     * @param message The text to convert
     * @return The converted text
     */
    public static String formUrls(String message) {
        return message.replaceAll(
            "(?<link>https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]+\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*))",
            "{\\$(click:open_url,${link})\\$(hover:show_text,&(aqua)Follow Link)&(aqua,underline)${link}}"
        );
    }

    /**
     * Replace command usage in chat with interactive commands <br>
     * `/&lt;command&gt; [args]` -> suggest exact command<br>
     * cmd:&lt;command&gt; -> Show full usage and advanced hover information
     * @param message The message to do replacements
     * @return The replaced message
     */
    private static String formCommandSuggestions(String message) {
        message = message.replaceAll("`(/.+)`", "{\\$(click:suggest_command,$1)\\$(hover:show_text,&(blue)Click to fill)&(aqua,underline)$1}");
        message = Pattern.compile("`cmd:(.+)`").matcher(message).replaceAll(Chat::matchCommand);

        return message;
    }

    private static String matchCommand(MatchResult result) {
        Command cmd = FarLands.getCommandHandler().getCommand(result.group(1));

        // Don't allow commands that don't exist or staff commands
        if(cmd == null || cmd.getMinRankRequirement().isStaff()) {
            return result.group(1);
        }

        Rank rank = cmd.getMinRankRequirement();
        StringBuilder desc =
            new StringBuilder("&(blue)Click to fill\n")
                .append("{&(gold)Usage: &(aqua,underline)").append(cmd.getUsage()).append("}\n")
                .append("&(gray)").append(cmd.getDescription());

        if (rank != Rank.INITIATE) {
            desc.append('\n').append("{&(gold)Rank Required: {&(").append(rank.getColor().getName()).append(")").append(rank.getName());
        }

        return String.format(
            "{\\$(click:suggest_command,/%s)\\$(hover:show_text,%s)&(aqua,underline)%s}",
            cmd.getLabel(),
            desc,
            cmd.getUsage()
        );
    }

    public static String applyEmotes(String message) {
        for (CommandShrug.TextEmote emote : CommandShrug.TextEmote.values) {
            message = message.replaceAll("(?i)(?<!\\\\)(:" + Pattern.quote(emote.name()) + ":)", TextUtils2.escapeExpression(emote.getValue()));
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
