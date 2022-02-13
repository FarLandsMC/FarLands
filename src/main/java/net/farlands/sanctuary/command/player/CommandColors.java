package net.farlands.sanctuary.command.player;

import com.google.common.collect.ImmutableMap;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CommandColors extends PlayerCommand {

    public CommandColors() {
        super(CommandData
            .withRank("colors", "Show available color codes for chat and signs.", "/colors", Rank.ADEPT)
            .aliases(false, "colours")
            .category(Category.COSMETIC)
        );
    }

    public static final Map<Character, Style> CHAR_STYLES = new ImmutableMap.Builder<Character, Style>()
//        .put('0', Style.style(NamedTextColor.BLACK))
        .put('1', Style.style(NamedTextColor.DARK_BLUE))
        .put('2', Style.style(NamedTextColor.DARK_GREEN))
        .put('3', Style.style(NamedTextColor.DARK_AQUA))
        .put('4', Style.style(NamedTextColor.DARK_RED))
        .put('5', Style.style(NamedTextColor.DARK_PURPLE))
        .put('6', Style.style(NamedTextColor.GOLD))
        .put('7', Style.style(NamedTextColor.GRAY))
        .put('8', Style.style(NamedTextColor.DARK_GRAY))
        .put('9', Style.style(NamedTextColor.BLUE))
        .put('a', Style.style(NamedTextColor.GREEN))
        .put('b', Style.style(NamedTextColor.AQUA))
        .put('c', Style.style(NamedTextColor.RED))
        .put('d', Style.style(NamedTextColor.LIGHT_PURPLE))
        .put('e', Style.style(NamedTextColor.YELLOW))
        .put('f', Style.style(NamedTextColor.WHITE))
//        .put('k', Style.style(TextDecoration.OBFUSCATED))
        .put('l', Style.style(TextDecoration.BOLD))
        .put('m', Style.style(TextDecoration.STRIKETHROUGH))
        .put('n', Style.style(TextDecoration.UNDERLINED))
        .put('o', Style.style(TextDecoration.ITALIC))
        .put('r', Style.empty())
        .build();

    public static final Component COLOR_MESSAGE;

    static {
        TextComponent.Builder builder = Component.text().color(NamedTextColor.GOLD).content("Legacy Color Codes: ");

        CHAR_STYLES.forEach(
            (k, v) -> builder.append(Component.text("&" + k).style(v)).append(Component.space())
        );

        builder.append(Component.newline())
            .append(Component.text("Hexadecimal Colors: "))
            .append(Component.join(
                JoinConfiguration.separators(ComponentColor.gold(", "), ComponentColor.gold(", or ")),
                Stream.of("&#rrggbb", "&#rgb", "<#rrggbb>").map(ComponentColor::aqua).toList()
            ))
            .append(Component.text(", like "))
            .append(Component.join(
                JoinConfiguration.separators(ComponentColor.gold(", "), ComponentColor.gold(", or ")),
                List.of(
                    Component.text("&#92b9bd", TextColor.color(0x92b9bd)),
                    Component.text("&#5f7", TextColor.color(0x55ff77)),
                    Component.text("<#1eae98>", TextColor.color(0x1eae98))
                )
            ))
            .append(Component.newline())
            .append(
                Component.text("FarLands also supports MiniMessage colors and gradients, which can be found ")
                    .append(ComponentUtils.link("here", "https://docs.adventure.kyori.net/minimessage"))
                    .append(Component.text("."))
            );

        COLOR_MESSAGE = builder.build();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sender.sendMessage(COLOR_MESSAGE);
        return true;
    }
}
