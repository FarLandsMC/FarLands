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

import java.util.regex.Pattern;

public class ChatFormat {

    // Patterns
    private static final Pattern LINK = Pattern.compile("(?i)(?<link>https?://(www\\.)?[-a-z0-9@:%._+~#=]+\\.[a-z0-9()]{1,6}\\b([-a-z0-9()@:%_+.~#?&/=]*))");
    private static final Pattern PING = Pattern.compile("(?i)(?<!\\\\)(@[a-z_]+)");
    private static final Pattern ITEM = Pattern.compile("(?i)(?<!\\\\)(\\[i(tem)?])");
    private static final Pattern COMMAND = Pattern.compile("(?i)(?<!\\\\)(`[^`]+`)");

    public static Component translateEmotes(Component message) {
        for (CommandShrug.TextEmote emote : CommandShrug.TextEmote.values) {
            message = message.replaceText(TextReplacementConfig.builder().match(Pattern.compile("(?i)(?<!\\\\)(:"
                                                                                                + Pattern.quote(emote.name()) + ":)")).replacement(emote.getValue()).build());
        }
        return message;
    }

    public static Component translateLinks(Component message) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(LINK)
                .replacement((result, input) -> ComponentUtils.link(result.group()))
                .build()
        );
    }

    public static Component translatePings(Component message, OfflineFLPlayer sender, boolean silent) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(PING)
                .replacement((result, input) -> {
                    String name = result.group().substring(1);
                    OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(name);
                    if (flp == null) {
                        return Component.text(result.group());
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

                    return Component.text("@" + flp.username)
                        .color(flp.rank.nameColor())
                        .hoverEvent(HoverEvent.showText(CommandStats.getFormattedStats(flp, false)));
                })
                .build()
        );
    }

    public static Component translateItems(Component message, Player sender) {
        if (sender == null) return message;
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(ITEM)
                .replacement((result, input) -> {
                    ItemStack item = sender.getEquipment().getItemInMainHand();
                    if (item.getType() == Material.AIR) {
                        item = sender.getEquipment().getItemInOffHand();
                    }

                    if (item.getType() == Material.AIR) {
                        return Component.text(result.group());
                    }

                    return ComponentColor.aqua("[i] ").append(ComponentUtils.item(item));
                })
                .build()
        );
    }

    public static Component translateCommands(Component message) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(COMMAND)
                .replacement((result, input) -> ComponentUtils.suggestCommand(result.group().replaceAll("`", "")))
                .build()
        );
    }
}
