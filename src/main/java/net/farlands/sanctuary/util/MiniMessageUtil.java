package net.farlands.sanctuary.util;

import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for better support when dealing with MiniMessage.
 */
public class MiniMessageUtil {

    // Patterns
    public static final Pattern LEGACY_HEX = Pattern.compile("(?i)(§x(§[0-9a-fl-or]){6})"); // Matches §x§r§r§g§g§b§b

    // MiniMessage tags banned for players
    public static final List<String> BANNED_COLORS = List.of("black", "obfuscated", "obf");

    // Color Code : MiniMessage Tag
    public static final Map<Character, String> CHAR_COLORS = new ImmutableMap.Builder<Character, String>()
        .put('0', "black")
        .put('1', "dark_blue")
        .put('2', "dark_green")
        .put('3', "dark_aqua")
        .put('4', "dark_red")
        .put('5', "dark_purple")
        .put('6', "gold")
        .put('7', "gray")
        .put('8', "dark_gray")
        .put('9', "blue")
        .put('a', "green")
        .put('b', "aqua")
        .put('c', "red")
        .put('d', "light_purple")
        .put('e', "yellow")
        .put('f', "white")
        .put('k', "obfuscated")
        .put('l', "bold")
        .put('m', "strikethrough")
        .put('n', "underlined")
        .put('o', "italic")
        .put('r', "reset")
        .build();

    /**
     * Convert from legacy spigot color codes (§#) to current MiniMessage codes
     *
     * @param allowBlocked Keep codes that are in the blocked list
     * @return
     */
    public static String fromLegacy(String message, boolean allowBlocked) {
        message = LEGACY_HEX
            .matcher(message)
            .replaceAll(mr -> {
                String code = mr.group().replaceAll("[§x]", "");

                if (
                    BANNED_COLORS.contains(
                        NamedTextColor.nearestTo(
                            TextColor.color(Integer.parseInt(code, 16)) // If it's close to a banned color, then block it
                        ).toString().toLowerCase()
                    )
                ) {
                    return ""; // "" rather than "$1" to not break up chat
                }
                return "<#" + code + ">";
            });

        message = Pattern.compile("(?i)(§[0-9a-fl-or])") // Matches: §#
            .matcher(message)
            .replaceAll(mr -> {
                String c = MiniMessageUtil.CHAR_COLORS.getOrDefault(mr.group().charAt(1), null);
                if (c == null || MiniMessageUtil.BANNED_COLORS.contains(c) && !allowBlocked) {
                    return "$1";
                } else {
                    return "<" + c + ">";
                }
            });

        return message;

    }

}
