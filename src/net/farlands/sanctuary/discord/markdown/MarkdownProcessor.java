package net.farlands.sanctuary.discord.markdown;

import net.md_5.bungee.api.ChatColor;

import java.util.*;
import java.util.regex.Pattern;

public class MarkdownProcessor {
    private static final CharacterProtector HTML_PROTECTOR = new CharacterProtector();
    private static final CharacterProtector CHAR_PROTECTOR = new CharacterProtector();
    public static final Map<ChatColor, String> CHATCOLOR_MARKDOWN = new HashMap<>();

    static {
        CHATCOLOR_MARKDOWN.put(ChatColor.BOLD, "**");
        CHATCOLOR_MARKDOWN.put(ChatColor.ITALIC, "*");
        CHATCOLOR_MARKDOWN.put(ChatColor.UNDERLINE, "__");
        CHATCOLOR_MARKDOWN.put(ChatColor.STRIKETHROUGH, "~~");
    }

    /**
     * Perform the conversion from Minecraft's formatting to Markdown
     * @param text The formatted text to convert
     * @return     The converted markdown
     */
    public static String mcToMarkdown(String text, int start) {
        if (start >= text.length()) {
            start = 0;
        }

        if (!text.substring(start).matches(".*[&" + ChatColor.COLOR_CHAR + "][0-9A-Fa-fk-oK-OrR].*")) {
            return text;
        }

        char[] chars = text.substring(start).toCharArray();

        List<ChatColor> usedColors = new ArrayList<>();
        StringBuilder outText = new StringBuilder();

        boolean skipNext = false;
        for (int i = 0; i < chars.length-1; i++) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            char c = chars[i];
            char nextChar = chars[i+1];
            if (c == ChatColor.COLOR_CHAR && ChatColor.ALL_CODES.contains(nextChar + "")) {
                skipNext = true;
                ChatColor color = ChatColor.getByChar(nextChar);
                if (CHATCOLOR_MARKDOWN.containsKey(color)) {
                    outText.append(CHATCOLOR_MARKDOWN.get(color));
                    usedColors.add(color);
                } else {
                    for (int j = usedColors.size() - 1; j >= 0; j--) {
                        outText.append(CHATCOLOR_MARKDOWN.get(usedColors.get(j)));
                    }
                    usedColors = new ArrayList<>();
                }
            } else {
                outText.append(c);
            }

        }
        for (int j = usedColors.size() - 1; j >= 0; j--) {
            outText.append(CHATCOLOR_MARKDOWN.get(usedColors.get(j)));
        }

        return text.substring(0, start) + outText.toString();
    }

    /**
     * Perform the conversion from Markdown to Minecraft's formatting.
     *
     * <ul>
     * Currently Handles:
     *     <li>**txt** -> Bold</li>
     *     <li>__txt__ -> Underline</li>
     *     <li>*txt* and _txt_ -> Italics</li>
     *     <li>~~txt~~ -> Strikethrough</li>
     *     <li>`txt` -> In-Line Code</li>
     *     <li>||txt|| -> Spoiler</li>
     * </ul>
     *
     * @param markdown input in Discord's markdown format
     * @return Minecraft Formatted text block from the text
     */
    public static String markdownToMC(String markdown) {
        if (markdown == null) {
            markdown = "";
        }
        TextEditor text = new TextEditor(markdown);

        text = text.detabify();
        text = text.deleteAll("^[ ]+$");
        text = formParagraphs(text);
        text = unEscapeSpecialChars(text);

        return text.toString();
    }

    private static TextEditor encodeBackslashEscapes(TextEditor text) {
        char[] normalChars = "`_>!".toCharArray();
        char[] escapedChars = "*{}[]()#+-.".toCharArray();

        // Two backslashes in a row
        text = text.replaceAllLiteral("\\\\\\\\", CHAR_PROTECTOR.encode("\\"));

        // Normal characters don't require a backslash in the regular expression
        text = encodeEscapes(text, normalChars, "\\\\");
        text = encodeEscapes(text, escapedChars, "\\\\\\");

        return text;
    }

    private static TextEditor encodeEscapes(TextEditor text, char[] chars, String slashes) {
        for (char ch : chars) {
            String regex = slashes + ch;
            text = text.replaceAllLiteral(regex, CHAR_PROTECTOR.encode(String.valueOf(ch)));
        }
        return text;
    }

    private static TextEditor formParagraphs(TextEditor markup) {
        markup = markup.deleteAll("\\A\\n+");
        markup = markup.deleteAll("\\n+\\z");

        String paragraph = markup.toString();
        String decoded = HTML_PROTECTOR.decode(paragraph);
        if (decoded != null) {
            paragraph = decoded;
        } else {
            paragraph = runSpanGamut(new TextEditor(paragraph)).toString();
        }
        return new TextEditor(paragraph);
    }

    private static TextEditor formUrls(TextEditor markup) {
        return markup.replaceAll(
                "(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])",
                "\\$(hoverlink,$1,&(aqua)Follow Link,&(aqua)$1)"
        );
    }

    private static TextEditor unEscapeSpecialChars(TextEditor ed) {
        for (String hash : CHAR_PROTECTOR.getAllEncodedTokens()) {
            String plaintext = CHAR_PROTECTOR.decode(hash);
            ed = ed.replaceAllLiteral(hash, plaintext);
        }
        return ed;
    }


    public static TextEditor runSpanGamut(TextEditor text) {
        text = escapeSpecialCharsWithinTagAttributes(text);
        text = encodeBackslashEscapes(text);
        text = escapeSpecialCharsWithinTagAttributes(text);
        text = formUrls(text);
        text = doMCFormat(text);
        return text;
    }

    private static TextEditor escapeSpecialCharsWithinTagAttributes(TextEditor text) {
        Collection<HTMLToken> tokens = text.tokenizeHTML();
        TextEditor newText = new TextEditor("");

        for (HTMLToken token : tokens) {
            String value = token.getText();
            if (token.isTag()) {
                value = value.replaceAll("\\\\", CHAR_PROTECTOR.encode("\\"));
                value = value.replaceAll("`", CHAR_PROTECTOR.encode("`"));
                value = value.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                value = value.replaceAll("_", CHAR_PROTECTOR.encode("_"));
            }
            newText.append(value);
        }
        return newText;
    }

    /**
     * Replaces the various Discord MarkDown into the Bukkit Versions<br>
     * **STRING** to (<b>STRING</b>)bold<br>
     * __STRING__ to (<u>STRING</u>)underline<br>
     * ~~STRING~~ to (<span style="text-decoration: line-through">STRING</span>)Strikethrough<br>
     * *STRING* or _STRING_ to (<em>STRING</em>) Italics
     * ||STRING|| to (Spoiler: ████)the spoiler format with a hover text
     * @param markup Markup TextEditor Object
     * @return The formatted version of the input
     */
    private static TextEditor doMCFormat(TextEditor markup) {
        markup = markup.replaceAll("(\\*\\*)(?=\\S)(.+?[*]*)(?<=\\S)\\1", ChatColor.BOLD + "$2" + ChatColor.RESET); // bold
        markup = markup.replaceAll("(__)(?=\\S)(.+?[_]*)(?<=\\S)\\1", ChatColor.UNDERLINE + "$2" + ChatColor.RESET); // underline
        markup = markup.replaceAll("(~~)(?=\\S)(.+?[~]*)(?<=\\S)\\1", ChatColor.STRIKETHROUGH + "$2" + ChatColor.RESET); // strikethrough
        markup = markup.replaceAll("(\\*|_)(?=\\S)(.+?)(?<=\\S)\\1", ChatColor.ITALIC + "$2" + ChatColor.RESET); // italic
        markup = markup.replaceAll("(`)(?=\\S)(.+?[`]*)(?<=\\S)\\1", ChatColor.GRAY + "$2" + ChatColor.RESET); // in-line code
        markup = markup.replaceAll("(\\|\\|)(?=\\S)(.+?[|]*)(?<=\\S)\\1", formatSpoiler("$2", "\\$")); // spoilers
        return markup;
    }

    /**
     * Format a message as a spoiler
     * This means: replace text with "&(gray)Spoiler: ████"
     * and give it a hover component with the original text
     * @param str The string to make into a spoiler (The hover text)
     * @param dollarSignReplace The String to put instead of the dollar sign(used for escaping the dollar sign, for example)
     * @return Hover text string "<code>dollarSignReplace</code>(hover,<code>str</code>,&(gray)Spoiler: ████)"
     */
    private static String formatSpoiler(String str, String dollarSignReplace) {
        return String.format("%s(hover,%s,&(gray)Spoiler: ████)", dollarSignReplace, str);
    }

    /**
     * Format a message as a spoiler
     * This means: replace text with "&(gray)Spoiler: ████"
     * and give it a hover component with the original text
     * @param str The string to make into a spoiler (The hover text)
     * @return Hover text string "$(hover,<code>str</code>,&(gray)Spoiler: ████)"
     */
    private static String formatSpoiler(String str) {
        return formatSpoiler(str, "$");
    }
}
