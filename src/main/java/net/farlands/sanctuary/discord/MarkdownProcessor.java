package net.farlands.sanctuary.discord;

import com.google.common.collect.ImmutableMap;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Markdown Processor for handling Discord's Markdown syntax and Adventure's {@link Component}s
 */
public class MarkdownProcessor {

    //                                        \       *    _    ~~    ||    <    >    `
    private static final String[] ESCAPE = { "\\\\", "*", "_", "~~", "||", "<", ">", "`", };

    private static final Map<TextDecoration, String> DECOR_MD_MAP =
        new ImmutableMap.Builder<TextDecoration, String>()
            .put(TextDecoration.BOLD, "**")
            .put(TextDecoration.UNDERLINED, "__")
            .put(TextDecoration.ITALIC, "*")
            .put(TextDecoration.STRIKETHROUGH, "~~")
            .build();

    private static final char ESCAPE_CHAR = '\\';
    private static final char ZERO_WIDTH_SPACE = '\u200B';

    private static final String SPOILER_TEXT = "Spoiler: ████";

    /**
     * Convert from a string styled using Discord's Markdown syntax into {@link Component}s
     * <br>
     * TODO: Handle Links
     *
     * @param markdown String styled using Discord's flavour of Markdown
     * @return Components based on the inputted Markdown
     */
    public static Component toMinecraft(@NotNull String markdown) {
        Map<String, MarkdownStyle> symbolMap = MarkdownStyle.symbolTypeMap(); // Map of Strings to Symbols
        BuilderWrapper root = BuilderWrapper.of(Component.text()); // Initial base component

        Stack<BuilderWrapper> branch = new Stack<>();
        branch.push(root);

        Stack<MarkdownStyle> styles = new Stack<>();


        StringBuilder currentString = new StringBuilder();

        Character curr, next; // Tracking vars
        char[] chars = markdown.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            curr = chars[i];
            next = i + 1 == chars.length ? null : chars[i + 1];

            if (curr == ESCAPE_CHAR && next != null) {
                currentString.append(next);
                ++i;
                continue;
            }

            // curr [+ next] is a valid symbol
            if (symbolMap.containsKey(curr + "") || symbolMap.containsKey(curr + "" + next)) {
                boolean twoCharSymbol = symbolMap.containsKey(curr + "" + next);
                MarkdownStyle decor = symbolMap.get(curr + "" + (twoCharSymbol ? next : "")); // Style based on symbol
                MarkdownStyle currentStyle = styles.isEmpty() ? MarkdownStyle.NONE : styles.peek(); // Current style is the top of the stack or NONE

                BuilderWrapper c = currentStyle.from(currentString.toString()); // Create builder from current style/text
                currentString.setLength(0); // Clear builder
                branch.peek().append(c); // Add new as child to current top

                if (currentStyle != decor) { // Open tag
                    branch.push(c); // Add it to the end of the current branch
                    styles.push(decor); // Update the style

                } else { // Closing tag
                    styles.pop(); // Remove the current style
                    branch.pop(); // Remove the last one
                }

                if (twoCharSymbol) ++i; // Skip next character
            } else {
                currentString.append(curr);
            }
        }
        MarkdownStyle currentStyle = styles.isEmpty() ? MarkdownStyle.NONE : styles.peek();

        BuilderWrapper c = currentStyle.from(currentString.toString()); // Create builder from current style/text
        branch.peek().append(c); // Add new as child to current top


        return root.build(); // Build into components

    }

    /**
     * Convert from {@link Component}s into Discord's Markdown syntax
     *
     * @param component Components to convert
     * @return Markdown string
     */
    public static String fromMinecraft(Component component) {

        String symbols = DECOR_MD_MAP // Get applicable symbols
            .entrySet()
            .stream()
            .filter(e -> component.style().hasDecoration(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.joining());

        String childrenText = component.children() // Get the text for the children components
            .stream()
            .map(MarkdownProcessor::fromMinecraft)
            .collect(Collectors.joining());
        String content = "";
        if (component instanceof TextComponent tc) content = tc.content();
        else if (component instanceof TranslatableComponent tc) {
            content = "---";
            FarLands.getDebugger().echo(tc.key());
        };

        return symbols + escapeMarkdown(content) + childrenText + symbols;
    }

    /**
     * Escape all symbols used for markdown in the specified text
     *
     * @param text The text to escape
     * @return The escaped text
     */
    public static String escapeMarkdown(String text) {
        text = text.replaceAll("([*_`~\\\\|:>])", "\\\\$1");
        text = text.replaceAll("@", "@" + ZERO_WIDTH_SPACE);
        return text;
    }


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

    /**
     * Custom styles for use in markdown
     */
    private enum MarkdownStyle {

        BOLD("**"),
        UNDERLINED("__"),
        ITALIC("*", "_"),
        STRIKETHROUGH("~~"),
        SPOILER("||"),
        CODE_BLOCK("`"),
        NONE(),
        ;

        private final String[] symbols;
        private final Style style;

        MarkdownStyle(String... symbols) {
            this.symbols = symbols;
            Style style;
            try {
                style = Style.style(TextDecoration.valueOf(name()));
            } catch (IllegalArgumentException ignored) {
                style = null;
            }
            this.style = style;
        }

        /**
         * Create a build wrapper from specific text
         * <p>
         * The builder is styled based on the enum
         *
         * @param text The text to use for the builder
         * @return The new {@link BuilderWrapper}
         */
        public BuilderWrapper from(String text) {
            return BuilderWrapper.of(builderFrom(text));
        }

        /**
         * Create a builder from specific text
         * <p>
         * The builder is styled based on the enum
         *
         * @param text The text to use for the builder
         * @return The new {@link TextComponent.Builder}
         */
        private TextComponent.Builder builderFrom(String text) {
            TextComponent.Builder builder = Component.text().content(text);
            if (this.style != null) {
                builder.style(this.style);
                return Component.text().content(text).style(this.style);
            }
            return switch (this) {
                case SPOILER -> Component.text()
                    .content(SPOILER_TEXT)
                    .style(Style.style(NamedTextColor.GRAY))
                    .hoverEvent(
                        HoverEvent.showText(
                            ComponentColor.gray(text)
                        )
                    );
                case CODE_BLOCK -> Component.text().content(text).color(NamedTextColor.GRAY);
                default -> Component.text().content(text);
            };
        }

        /**
         * @return A map for the symbol to decoration
         */
        public static Map<String, MarkdownStyle> symbolTypeMap() {
            Map<String, MarkdownStyle> map = new HashMap<>();
            for (MarkdownStyle value : values()) {
                for (String symbol : value.symbols) {
                    map.put(symbol, value);
                }
            }
            return map;
        }

    }

    /**
     * A mutable wrapper for the {@link TextComponent.Builder}
     */
    private static class BuilderWrapper {

        private final TextComponent.Builder builder;
        private final List<BuilderWrapper> children;

        public BuilderWrapper(TextComponent.Builder builder) {
            this.builder = builder;
            this.children = new ArrayList<>();
        }

        /**
         * Add a child builder to this wrapper
         *
         * @param builder the builder to add
         */
        public void append(TextComponent.Builder builder) {
            this.children.add(new BuilderWrapper(builder));
        }


        /**
         * Add a child builder to this wrapper
         *
         * @param builder the builder to add
         */
        public void append(BuilderWrapper builder) {
            this.children.add(builder);
        }

        /**
         * Build this wrapper based on the children and builder
         *
         * @return The Component from building the children and itself
         */
        public Component build() {
            TextComponent.Builder builder = this.builder.build().toBuilder();
            this.children.stream().map(BuilderWrapper::build).forEach(builder::append);
            return builder.build();
        }

        /**
         * Create a {@link BuilderWrapper} from a specified builder
         *
         * @param builder The builder to wrap
         * @return A new BuilderWrapper
         */
        public static BuilderWrapper of(TextComponent.Builder builder) {
            return new BuilderWrapper(builder);
        }
    }

}
