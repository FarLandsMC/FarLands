package net.farlands.sanctuary.discord;

import com.google.common.collect.ImmutableMap;
import io.papermc.paper.text.PaperComponents;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatFormat;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang.StringUtils;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Markdown Processor for handling Discord's Markdown syntax and Adventure's {@link Component}s
 */
public class MarkdownProcessor {

    private static final PlainTextComponentSerializer serializer = PlainTextComponentSerializer.builder()
        .flattener(
            ComponentFlattener
                .builder()
                .mapper(Component.class, MarkdownProcessor::componentToMarkdown)
                .build()
        )
        .build();

    private static final Map<TextDecoration, String> DECOR_MD_MAP =
        new ImmutableMap.Builder<TextDecoration, String>()
            .put(TextDecoration.BOLD, "**")
            .put(TextDecoration.UNDERLINED, "__")
            .put(TextDecoration.ITALIC, "*")
            .put(TextDecoration.STRIKETHROUGH, "~~")
            .build();

    // RegEx : TextDecoration
    private static final Map<String, TextDecoration> SIMPLE_DECORATIONS = new ImmutableMap.Builder<String, TextDecoration>()
        .put("__", TextDecoration.UNDERLINED)
        .put("\\*\\*", TextDecoration.BOLD)
        .put("[*_]", TextDecoration.ITALIC)
        .put("~~", TextDecoration.STRIKETHROUGH)
        .build();

    private static final @Language("RegExp")
    String MARKDOWN_STRING = "(?<!\\\\)(%s)(.+?(?!\\1).)\\1(?!\\1)"; // https://regexr.com/6db8g -- `%s` gets replaced with the symbols -- group 2 is the content

    private static final char      ZERO_WIDTH_SPACE = '\u200B';
    private static final Component SPOILER_BASE     = ComponentColor.gray("████").decorate(TextDecoration.UNDERLINED);

    public static Component toMinecraft(@NotNull String markdown) {
        Component component = Component.text(markdown);

        for (Map.Entry<String, TextDecoration> entry : SIMPLE_DECORATIONS.entrySet()) {
            component = component.replaceText(
                TextReplacementConfig
                    .builder()
                    .match(Pattern.compile(MARKDOWN_STRING.formatted(entry.getKey())))
                    .replacement((mr, u) -> Component.text(mr.group(2)).decorate(entry.getValue()))
                    .build()
            );
        }

        component = component
            .replaceText(
                TextReplacementConfig // Spoiler Blocks
                    .builder()
                    .match(Pattern.compile(MARKDOWN_STRING.formatted("||")))
                    .replacement((mr, u) -> SPOILER_BASE.hoverEvent(HoverEvent.showText(ComponentColor.gray(mr.group(1)))))
                    .build()
            )
            .replaceText(
                TextReplacementConfig // User mentions
                    .builder()
                    .match(Pattern.compile("<@!?(\\d+)>"))
                    .replacement((mr, u) -> userMention(mr.group(1)))
                    .build()
            )
            .replaceText(
                TextReplacementConfig // Channel mentions
                    .builder()
                    .match(Pattern.compile("<#(\\d+)>"))
                    .replacement((mr, u) -> channelMention(mr.group(1)))
                    .build()
            )
            .replaceText(
                TextReplacementConfig // Emoji mentions
                    .builder()
                    .match(Pattern.compile("<a?:(.+?):(\\d+)>"))
                    .replacement((mr, u) -> Component.text(":" + mr.group(1) + ":"))
                    .build()
            );

        return component;
    }

    /**
     * Convert from {@link Component}s into Discord's Markdown syntax
     *
     * @param component Components to convert
     * @return Markdown string
     */
    public static String fromMinecraft(Component component) {

        if (component == null) return "";
        return serializer.serialize(component) // Serialize
            .replaceAll("@", "@" + ZERO_WIDTH_SPACE); // Prevent @everyone, @here, etc.
    }

    /**
     * Add the correct symbols for the styling
     */
    private static String componentToMarkdown(Component component) {
        String symbols = DECOR_MD_MAP // Get applicable symbols
            .entrySet()
            .stream()
            .filter(e -> component.style().hasDecoration(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.joining());
        String content = "";
        if (component instanceof TextComponent tc) {
            content = tc.content();
        } else if (component instanceof TranslatableComponent tc) {
            content = PlainTextComponentSerializer.builder().flattener(PaperComponents.flattener()).build().serialize(tc);
        }
        return symbols + escapeMarkdown(content) + StringUtils.reverse(symbols);
    }

    /**
     * Remove all § and & color codes
     *
     * @param source The source string
     * @return The cleaned string
     */
    public static String removeChatColor(String source) {
        return source.replaceAll("(?i)[&§][0-9a-fk-orx]", "");
    }

    /**
     * Escape all symbols used for markdown in the specified text
     *
     * @param text The text to escape
     * @return The escaped text
     */
    public static String escapeMarkdown(String text) {
        return text.replaceAll("([*_`~\\\\|:>])", "\\\\$1")
            .replaceAll("@", "@" + ZERO_WIDTH_SPACE);
    }

    public static Component userMention(String text) {

        long id = Long.parseLong(text);
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(id);
        if (flp != null) {
            return ChatFormat.playerMention(flp);
        }
        User user = FarLands.getDiscordHandler().getNativeBot().getUserById(id);
        if (user instanceof Member member) {
            return Component.text("@" + member.getEffectiveName());
        }
        if (user != null) {
            return Component.text("@" + user.getName());
        }
//        return Component.text(text); // If this run, something is funky
        return null;

    }

    public static Component channelMention(String text) {
        long id = Long.parseLong(text);
        GuildChannel channel = FarLands.getDiscordHandler().getGuild().getGuildChannelById(id);
        if (channel == null) {
            return null;
        }
        return Component.text("#" + channel.getName());
    }

//    public static Component emoteMention(String text) {
//        Message
//        net.dv8tion.jda.internal.utils.Helpers.
//    }


    /**
     * Perform the conversion from Minecraft's formatting to Discord's Markdown
     *
     * @param text  The formatted text to convert
     * @param start The index to start parsing from
     * @return The converted markdown
     * @deprecated Switch to {@link MarkdownProcessor#fromMinecraft(Component)}
     */
    @Deprecated
    public static String mcToMarkdown(String text, int start) {

        if (start >= text.length()) {
            start = 0;
        }

        Component c = LegacyComponentSerializer.legacySection().deserialize(text.substring(start));
        return text.substring(0, start) + fromMinecraft(c);
    }


    /**
     * Perform the conversion from Discord's Markdown to Minecraft's formatting.
     *
     * @param markdown input in Discord's markdown format
     * @return Minecraft Formatted text block from the text
     * @deprecated Switch to {@link MarkdownProcessor#toMinecraft(String)}
     */
    @Deprecated
    public static String markdownToMC(String markdown) {
        return LegacyComponentSerializer.legacySection().serialize(toMinecraft(markdown));
    }
}
