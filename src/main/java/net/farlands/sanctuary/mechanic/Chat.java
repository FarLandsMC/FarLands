package net.farlands.sanctuary.mechanic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kicas.rp.util.TextUtils2;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.player.CommandShrug;
import net.farlands.sanctuary.command.player.CommandStats;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.IgnoreStatus;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles everything to do with chat.
 * TODO: Remove this class -- some methods are still needed to be moved.
 */
public class Chat extends Mechanic {
    public static final List<ChatColor> ILLEGAL_COLORS = Arrays.asList(ChatColor.MAGIC, ChatColor.BLACK);
    public static final List<Character> COLOR_CHARS = Arrays.asList('0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'l', 'm', 'n', 'o', 'r', 'x');
    private static final List<String> DISCORD_CHARS = Arrays.asList("_", "~", "*", ":", "`", "|", ">");

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
        String hoverText = CommandStats.getFormattedStats(flp, false).toString()/*TODO:temp toString*/.replaceAll("([()])", "\\\\$1");
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
