package net.farlands.sanctuary.discord.markdown;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutable String with common operations used in Markdown processing.
 */
public class TextEditor {
    private StringBuilder text;

    /**
     * Create a new TextEditor based on the contents of a String or
     * StringBuffer.
     */
    public TextEditor(CharSequence text) {
        this.text = new StringBuilder(text);
    }

    @Override
    public String toString() {
        return text.toString();
    }

    /**
     * Replace all occurrences of the regular expression with the replacement.  The replacement string
     * can contain $1, $2 etc. referring to matched groups in the regular expression.
     */
    public TextEditor replaceAll(String regex, String replacement) {
        if (text.length() > 0) {
            Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = p.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
            text = new StringBuilder(sb.toString());
        }
        return this;
    }

    /**
     * Same as replaceAll(String, String), but does not interpret
     * $1, $2 etc. in the replacement string.
     */
    public TextEditor replaceAllLiteral(String regex, final String replacement) {
        return replaceAll(Pattern.compile(regex, Pattern.MULTILINE), m -> replacement);
    }

    /**
     * Replace all occurrences of the Pattern.  The Replacement object's replace() method is
     * called on each match, and it provides a replacement, which is placed literally
     * (i.e., without interpreting $1, $2 etc.)
     */
    public TextEditor replaceAll(Pattern pattern, Replacement replacement) {
        Matcher m = pattern.matcher(text);
        int lastIndex = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(text.subSequence(lastIndex, m.start()));
            sb.append(replacement.replacement(m));
            lastIndex = m.end();
        }
        sb.append(text.subSequence(lastIndex, text.length()));
        text = sb;
        return this;
    }

    /**
     * Remove all occurrences of the given regex pattern, replacing them
     * with the empty string.
     */
    public TextEditor deleteAll(String pattern) {
        return replaceAll(pattern, "");
    }

    /**
     * Convert tabs to spaces given the default tab width of 4 spaces.
     */
    public TextEditor detabify() {
        return detabify(4);
    }

    /**
     * Convert tabs to spaces.
     */
    public TextEditor detabify(final int tabWidth) {
        replaceAll(Pattern.compile("(.*?)\\t"), m -> {
            String lineSoFar = m.group(1);
            int width = lineSoFar.length();
            StringBuilder replacement = new StringBuilder(lineSoFar);
            do {
                replacement.append(' ');
                ++width;
            } while (width % tabWidth != 0);
            return replacement.toString();
        });
        return this;
    }

    /**
     * Remove a number of spaces at the start of each line.
     */
    public TextEditor outdent(int spaces) {
        return deleteAll("^(\\t|[ ]{1," + spaces + "})");
    }

    /**
     * Remove one tab width (4 spaces) from the start of each line.
     */
    public TextEditor outdent() {
        return outdent(4);
    }

    /**
     * Remove leading and trailing space from the start and end of the buffer.  Intermediate
     * lines are not affected.
     */
    public TextEditor trim() {
        text = new StringBuilder(text.toString().trim());
        return this;
    }

    /**
     * Add a string to the end of the buffer.
     */
    public void append(CharSequence s) {
        text.append(s);
    }

    /**
     * Parse HTML tags, returning a Collection of HTMLToken objects.
     */
    public Collection<HTMLToken> tokenizeHTML() {
        List<HTMLToken> tokens = new ArrayList<>();
        String nestedTags = nestedTagsRegex(6);

        Pattern p = Pattern.compile("" +
                "(?s:<!(--.*?--\\s*)+>)" +
                "|" +
                "(?s:<\\?.*?\\?>)" +
                "|" +
                nestedTags +
                "", Pattern.CASE_INSENSITIVE);

        Matcher m = p.matcher(text);
        int lastPos = 0;
        while (m.find()) {
            if (lastPos < m.start()) {
                tokens.add(HTMLToken.text(text.substring(lastPos, m.start())));
            }
            tokens.add(HTMLToken.tag(text.substring(m.start(), m.end())));
            lastPos = m.end();
        }
        if (lastPos < text.length()) {
            tokens.add(HTMLToken.text(text.substring(lastPos, text.length())));
        }

        return tokens;
    }

    /**
     * Regex to match a tag, possibly with nested tags such as <a href="<MTFoo>">.
     *
     * @param depth - How many levels of tags-within-tags to allow.  The example <a href="<MTFoo>"> has depth 2.
     */
    private String nestedTagsRegex(int depth) {
        if (depth == 0) {
            return "";
        } else {
            return "(?:<[a-z/!$](?:[^<>]|" + nestedTagsRegex(depth - 1) + ")*>)";
        }
    }
}
