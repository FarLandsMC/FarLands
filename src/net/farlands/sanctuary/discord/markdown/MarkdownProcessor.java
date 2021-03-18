package net.farlands.sanctuary.discord.markdown;

import net.md_5.bungee.api.ChatColor;

import java.util.Collection;

public class MarkdownProcessor {
    private static final CharacterProtector HTML_PROTECTOR = new CharacterProtector();
    private static final CharacterProtector CHAR_PROTECTOR = new CharacterProtector();

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
    public String markdown(String markdown) {
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

    private TextEditor encodeBackslashEscapes(TextEditor text) {
        char[] normalChars = "`_>!".toCharArray();
        char[] escapedChars = "*{}[]()#+-.".toCharArray();

        // Two backslashes in a row
        text = text.replaceAllLiteral("\\\\\\\\", CHAR_PROTECTOR.encode("\\"));

        // Normal characters don't require a backslash in the regular expression
        text = encodeEscapes(text, normalChars, "\\\\");
        text = encodeEscapes(text, escapedChars, "\\\\\\");

        return text;
    }

    private TextEditor encodeEscapes(TextEditor text, char[] chars, String slashes) {
        for (char ch : chars) {
            String regex = slashes + ch;
            text = text.replaceAllLiteral(regex, CHAR_PROTECTOR.encode(String.valueOf(ch)));
        }
        return text;
    }

    private TextEditor formParagraphs(TextEditor markup) {
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

    private TextEditor formUrls(TextEditor markup) {
        return markup.replaceAll(
                "(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])",
                "\\$(hoverlink,$1,&(aqua)Follow Link,&(aqua)$1)"
        );
    }

    private TextEditor unEscapeSpecialChars(TextEditor ed) {
        for (String hash : CHAR_PROTECTOR.getAllEncodedTokens()) {
            String plaintext = CHAR_PROTECTOR.decode(hash);
            ed = ed.replaceAllLiteral(hash, plaintext);
        }
        return ed;
    }


    public TextEditor runSpanGamut(TextEditor text) {
        text = escapeSpecialCharsWithinTagAttributes(text);
        text = encodeBackslashEscapes(text);
        text = escapeSpecialCharsWithinTagAttributes(text);
        text = formUrls(text);
        text = doMCFormat(text);
        return text;
    }

    private TextEditor escapeSpecialCharsWithinTagAttributes(TextEditor text) {
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
    private TextEditor doMCFormat(TextEditor markup) {
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
    private String formatSpoiler(String str, String dollarSignReplace) {
        return String.format("%s(hover,%s,&(gray)Spoiler: ████)", dollarSignReplace, str);
    }

    /**
     * Format a message as a spoiler
     * This means: replace text with "&(gray)Spoiler: ████"
     * and give it a hover component with the original text
     * @param str The string to make into a spoiler (The hover text)
     * @return Hover text string "$(hover,<code>str</code>,&(gray)Spoiler: ████)"
     */
    private String formatSpoiler(String str) {
        return formatSpoiler(str, "$");
    }
}
