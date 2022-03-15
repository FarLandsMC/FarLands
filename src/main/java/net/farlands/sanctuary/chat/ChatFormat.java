package net.farlands.sanctuary.chat;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandShrug;
import net.farlands.sanctuary.command.player.CommandStats;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Language;

import java.util.function.Function;
import java.util.regex.Pattern;

public class ChatFormat {

    // Patterns
    private static final Pattern LINK    = Pattern.compile("(?i)(?<link>\\\\?https?://(www\\.)?[-a-z0-9@:%._+~#=]+\\.[a-z0-9()]{1,6}\\b([-a-z0-9()@:%_+.~#?&/=]*))"); // Overcomplicated RegExp for links, but why not?
    private static final Pattern PING    = Pattern.compile("(?i)\\\\?@[a-z_\\d]{3,}"); // Matches \?@<playername>
    private static final Pattern ITEM    = Pattern.compile("(?i)\\\\?\\[i(tem)?]"); // Matches \?[i]
    private static final Pattern COMMAND = Pattern.compile("(?i)\\\\?`/.+?`"); // Matches \?`command`
    private static final @Language("RegExp")
                         String  EMOTE   = "(?i)\\\\?:%EMOTE%:"; // %EMOTE% gets replaced with the emote name

    public static Component translateAll(Component component, OfflineFLPlayer sender) {
        component = ChatFormat.translateEmotes(component); // :shrug: -> ¯\_(ツ)_/¯
        component = ChatFormat.translateLinks(component); // Highlight Links
        component = ChatFormat.translatePings(component, sender, false); // Make @<name> have hover text with stats
        component = ChatFormat.translateCommands(component); // `cmd` -> cmd (with hover and click event)
        component = ChatFormat.translateItems(component, sender.getOnlinePlayer()); // [i] -> "[i] <name>" with hover text
        return component;
    }

    public static Component translateEmotes(Component message) {
        for (CommandShrug.TextEmote emote : CommandShrug.TextEmote.values) {
            message = message
                .replaceText(
                    TextReplacementConfig
                        .builder()
                        .match(Pattern.compile(EMOTE.replace("%EMOTE%", emote.name())))
                        .replacement((result, input) -> escapeOr(result.group(), n -> Component.text(emote.getValue())))
                        .build()
                );
        }
        return message;
    }

    public static Component translateLinks(Component message) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(LINK)
                .replacement((result, input) -> escapeOr(result.group(), ComponentUtils::link))
                .build()
        );
    }

    public static Component translatePings(Component message, OfflineFLPlayer sender, boolean silent) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(PING)
                .replacement((result, input) -> escapeOr(result.group(), text -> {
                    String name = text.substring(1);
                    OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(name);
                    if (flp == null) {
                        return Component.text(text);
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

                    return playerMention(flp);
                }))
                .build()
        );
    }

    public static Component translateItems(Component message, Player sender) {
        if (sender == null) return message;
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(ITEM)
                .replacement((result, input) -> escapeOr(result.group(), text -> {
                    ItemStack item = sender.getEquipment().getItemInMainHand();
                    if (item.getType() == Material.AIR) {
                        item = sender.getEquipment().getItemInOffHand();
                    }

                    if (item.getType() == Material.AIR) {
                        return Component.text(text);
                    }

                    return ComponentColor.aqua("[i] ").append(ComponentUtils.item(item));
                }))
                .build()
        );
    }

    public static Component translateCommands(Component message) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(COMMAND)
                .replacement((result, input) -> escapeOr(result.group(), text -> ComponentUtils.suggestCommand(text.replaceAll("`", ""))))
                .build()
        );
    }

    /**
     * Returns an escaped message if it starts with '\'
     *
     * @param message
     * @param ifNotEscaped Method to run on input if string is not escaped
     * @return Plain text component if not escaped, otherwise a component generated by ifNotEscaped function
     */
    public static Component escapeOr(String message, Function<String, Component> ifNotEscaped) {
        return message.startsWith("\\") ? Component.text(message.substring(1)) : ifNotEscaped.apply(message);

    }

    public static Component playerMention(OfflineFLPlayer flp) {
        return Component.text("@" + flp.username)
            .color(flp.rank.nameColor())
            .hoverEvent(HoverEvent.showText(CommandStats.getFormattedStats(flp, false)));
    }
}
