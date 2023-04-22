package net.farlands.newheights;

import net.farlands.sanctuary.command.player.CommandShrug;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class ChatFormatTest {

    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    @Test
    public void testEmotes() {
        Component component = Component.text( ":shrug: and :shru:");
        Assert.assertEquals(
            "¯\\_(ツ)_/¯ and :shru:",
            plainText.serialize(translateEmotes(component))
        );
    }

    public Component translateEmotes(Component message) {
        for (CommandShrug.TextEmote emote : CommandShrug.TextEmote.values) {
            message = message.replaceText(TextReplacementConfig.builder().match(Pattern.compile("(?i)(?<!\\\\)(:"
                + Pattern.quote(emote.name()) + ":)")).replacement(emote.getValue()).build());
        }
        return message;
    }

    @Test
    public void testLinks() {
        String string = "this contains a link https://google.com pog";
        Assert.assertEquals(
            Component.text("this contains a link ")
                .append(
                    Component.text("https://google.com")
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl("https://google.com"))
                        .hoverEvent(HoverEvent.showText(ComponentColor.gray("Open Link")))
                )
                .append(Component.text(" pog")),
            translateLinks(Component.text(string))
        );
    }

    private static final Pattern LINK = Pattern.compile("(?i)(?<link>https?://(www\\.)?[-a-z0-9@:%._+~#=]+\\.[a-z0-9()]{1,6}\\b([-a-z0-9()@:%_+.~#?&/=]*))");

    public Component translateLinks(Component message) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(LINK)
                .replacement((result, input) -> ComponentUtils.link(result.group()))
                .build()
        );
    }

    @Test
    public void testCommands() {
        String string = "make sure to use `/afk` when you leave!";
        Assert.assertEquals(
            Component.text("make sure to use ")
                .append(
                    Component.text("/afk")
                        .color(NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.suggestCommand("/afk"))
                        .hoverEvent(HoverEvent.showText(
                            Component.text("Fill Command")
                                .color(NamedTextColor.GRAY)
                        ))
                )
                .append(
                    Component.text(" when you leave!")
                ),
            translateCommands(Component.text(string))
        );
    }

    private static final Pattern COMMAND = Pattern.compile("(?i)(?<!\\\\)(`[^`]+`)");

    public Component translateCommands(Component message) {
        return message.replaceText(
            TextReplacementConfig.builder()
                .match(COMMAND)
                .replacement((result, input) -> ComponentUtils.suggestCommand(result.group().replaceAll("`", "")))
                .build()
        );
    }
}
