package net.farlands.sanctuary.chat;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandShrug;
import net.farlands.sanctuary.command.player.CommandStats;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.regex.Pattern;

public class ChatFormat {

    // Patterns
    private static final Pattern CHAR_COLOR = Pattern.compile("(?i)(&[0-9a-fl-or])");
    private static final Pattern HEX_COLOR = Pattern.compile("(?i)(&#(([a-f0-9]{6})|([a-f0-9]{3})))");
    private static final Pattern EMOTE = Pattern.compile("(?i)(?<!\\\\)(:[a-z]+:)");
    private static final Pattern LINK = Pattern.compile("(?i)(?<link>https?://(www\\.)?[-a-z0-9@:%._+~#=]+\\.[a-z0-9()]{1,6}\\b([-a-z0-9()@:%_+.~#?&/=]*))");
    private static final Pattern PING = Pattern.compile("(?i)(?<!\\\\)(@[a-z_]+)");
    private static final Pattern ITEM = Pattern.compile("(?i)(?<!\\\\)(\\[i(tem)?])");
    private static final Pattern COMMAND = Pattern.compile("(?i)(?<!\\\\)(`[^`]+`)");

    public static String translateColors(String message, OfflineFLPlayer sender) {
        if (sender.rank.specialCompareTo(Rank.KNIGHT) < 0) {
            return message; // Don't translate colors if the sender is not Knight+
        }

        message = CHAR_COLOR
            .matcher(message)
            .replaceAll(
                mr -> {
                    String c = MiniMessageUtil.CHAR_COLORS.getOrDefault(mr.group().charAt(1), null);
                    if (c == null || MiniMessageUtil.BANNED_COLORS.contains(c) && !sender.rank.isStaff()) { // Block colors, unless staff
                        return "$1";
                    } else {
                        return '<' + c + '>';
                    }
                });

        message = HEX_COLOR
            .matcher(message)
            .replaceAll(
                mr -> {
                    String hex = mr.group().replaceAll("&#", "");
                    if (hex.length() == 3) {
                        char r = hex.charAt(0);
                        char g = hex.charAt(1);
                        char b = hex.charAt(2);
                        hex = "" + r + r + g + g + b + b; // #rgb -> #rrggbb
                    }

                    // If the color is close to black, prevent it from showing
                    if (NamedTextColor.nearestTo(TextColor.color(Integer.parseInt(hex, 16))) == NamedTextColor.BLACK) {
                        return ""; // "" rather than "$1" to not break up chat with color codes
                    }

                    return "<#" + hex + ">";
                });


        return message;
    }

    public static String translateEmotes(String message) {
        return EMOTE.matcher(message).replaceAll(mr -> {
            String emote = mr.group().replaceAll(":", "");
            try {
                return CommandShrug.TextEmote.valueOf(emote.toUpperCase()).getValue().replaceAll("\\\\", "\\\\\\\\");
            } catch (IllegalArgumentException ex) {
                return "$1";
            }
        });
    }

    public static String translateLinks(String message) {
        return LINK.matcher(message).replaceAll(mr -> {
            return MiniMessage.miniMessage().serialize(ComponentUtils.link(mr.group()))
                + "</click></hover></underlined></aqua>"; // TODO: Remove when https://github.com/KyoriPowered/adventure-text-minimessage/issues/171 is fixed
        });
    }

    public static String translatePing(String message, OfflineFLPlayer sender, boolean silent) {
        return PING.matcher(message).replaceAll(mr -> {
            String name = mr.group().substring(1);
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(name);
            if (flp == null) {
                return "$1";
            }

            if (!silent && flp.getOnlinePlayer() != null && !flp.getIgnoreStatus(sender).includesChat()) {
                flp.getOnlinePlayer() // Play 'ping' noise
                    .playSound(
                        flp.getOnlinePlayer().getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        6.0F,
                        1.0F
                    );
            }

            // Use <c:#rrggbb> tag style, so it's easier to close with </c>
            String str = "<c:" + flp.rank.nameColor().asHexString() + ">"; // TODO: Update when https://github.com/KyoriPowered/adventure-text-minimessage/issues/171 is fixed

            str += MiniMessage.miniMessage().serialize(
                Component.text("@" + flp.username)
                    .hoverEvent(
                        HoverEvent.showText(CommandStats.getFormattedStats(flp, false))
                    )
            );

            str += "</hover></c>"; // TODO: Remove when https://github.com/KyoriPowered/adventure-text-minimessage/issues/171 is fixed
            return str;
        });
    }

    public static String translateItem(String message, Player player) {
        return ITEM.matcher(message).replaceAll(mr -> {
            ItemStack item = player.getEquipment().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                item = player.getEquipment().getItemInOffHand();
            }

            if (item.getType() == Material.AIR) return "$1";

            return MiniMessage.miniMessage().serialize(
                ComponentColor.aqua("[i] ")
                    .append(ComponentUtils.item(item)))
                + "</hover></aqua>";
        });

    }

    public static String translateCommands(String message) {
        return COMMAND.matcher(message).replaceAll(mr -> {
            return MiniMessage.miniMessage().serialize(ComponentUtils.suggestCommand(mr.group().replaceAll("`", "")))
                + "</click></hover></aqua>"; // TODO: Remove when https://github.com/KyoriPowered/adventure-text-minimessage/issues/171 is fixed
        });
    }
}
