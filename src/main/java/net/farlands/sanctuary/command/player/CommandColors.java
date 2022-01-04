package net.farlands.sanctuary.command.player;

import com.google.common.collect.ImmutableMap;
import net.farlands.sanctuary.chat.MiniMessageWrapper;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.kyori.adventure.text.minimessage.placeholder.Placeholder;
import net.kyori.adventure.text.minimessage.placeholder.PlaceholderResolver;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandColors extends PlayerCommand {
    public CommandColors() {
        super(Rank.ADEPT, Category.COSMETIC, "Show available color codes for chat and signs.", "/colors", "colors", "colours");
    }

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

    @Override
    public boolean execute(Player sender, String[] args) {
        List<ChatColor> chatColors = Arrays.stream(ChatColor.values()).collect(Collectors.toList());
        chatColors.remove(ChatColor.MAGIC);
        chatColors.remove(ChatColor.BLACK);
        MiniMessageWrapper wrapper = MiniMessageWrapper.legacy().toBuilder().advancedTransformations(true).build();
        sender.sendMessage(wrapper.mmParse(
            "<gold>Legacy Color Codes:</gold> " + chatColors.stream().map(chatColor ->
                "<" + CHAR_COLORS.getOrDefault(chatColor.getChar(), "white") + ">" + "\\&"
                    + chatColor.getChar() + "</" + CHAR_COLORS.getOrDefault(chatColor.getChar(), "white") + ">"
            ).collect(Collectors.joining(" "))
        ));
        PlaceholderResolver resolver = PlaceholderResolver.placeholders(
            Placeholder.miniMessage("hex_ex_1", "&#92b9bd"),
            Placeholder.miniMessage("hex_ex_2", "&#5f7")
        );
        sender.sendMessage(wrapper.toBuilder().placeholderResolver(resolver).build().mmParse(
            "<gold>Hexadecimal Colors: &#rrggbb, &#rgb, or \\<#rrggbb>, like <c:#92b9bd><hex_ex_1></c>, " +
                "<c:#55ff77><hex_ex_2></c>, or <c:#1eae98>\\<#1eae98></c>."
        ));
        sender.sendMessage(wrapper.mmParse(
            "<gold>FarLands also supports all other MiniMessage color codes, including gradients. " +
                "Read their documentation <click:open_url:'https://docs.adventure.kyori.net/minimessage'><aqua>here</aqua></click>."
        ));
        return true;
    }
}
