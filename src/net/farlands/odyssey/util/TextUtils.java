package net.farlands.odyssey.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides utilities for a more complex text component formatting system.
 */
public class TextUtils {
    private static final char VALUE_MARKER = '%';
    private static final char COLOR_CHAR = '&';
    private static final char SECTION_START = '{';
    private static final char FUNCTION_CHAR = '$';

    /**
     * Parses the given input text and substitutes in the given values and sends the result to the given command sender.
     *
     * @param sender the recipient of the formatted message.
     * @param input  the input text.
     * @param values the values to substitute in.
     */
    public static void sendFormatted(CommandSender sender, String input, Object... values) {
        sender.spigot().sendMessage(format(input, values));
    }

    /**
     * Parses the given input text and substitutes in the given values and returns the result as an array of base
     * components.
     *
     * @param input  the input text.
     * @param values the values to substitute in.
     * @return the parsed input text as an array of base components.
     */
    public static BaseComponent[] format(String input, Object... values) {
        return parseExpression(new Pair<>(ChatColor.WHITE, new ArrayList<>()), insertValues(input, values), values);
    }

    /**
     * Parses the given input text and returns the result as an array of base components.
     *
     * @param input the input text.
     * @return the parsed input text as an array of base components.
     */
    public static BaseComponent[] format(String input) {
        return parseExpression(new Pair<>(ChatColor.WHITE, new ArrayList<>()), input);
    }

    // Insert values into the raw input
    public static String insertValues(String raw, Object... values) {
        if (values.length == 0)
            return raw;

        StringBuilder sb = new StringBuilder(raw.length());
        char[] chars = raw.toCharArray();
        char cur, next;

        for (int i = 0; i < chars.length; ++i) {
            cur = chars[i];
            next = i < chars.length - 1 ? chars[i + 1] : '\0';

            // Ignore escaped characters
            if (cur == '\\' && next == VALUE_MARKER) {
                sb.append(VALUE_MARKER);
                ++i;
            } else if (cur == VALUE_MARKER) { // Insert a value
                // Use hex for more indices
                int index = Character.digit(next, 16);
                if (index < values.length)
                    sb.append(values[index]);
                ++i;
            } else // Just append the next character
                sb.append(cur);
        }

        return sb.toString();
    }

    // Parses the given expression. The format variable is as follows: text color, additive format colors (such as bold,
    // italic, etc.). The values variable is the list of values potentially substituted into the text, and is used by
    // the inflecting function.
    private static BaseComponent[] parseExpression(Pair<ChatColor, List<ChatColor>> format, String input,
                                                   Object... values) {
        // Current component text
        StringBuilder component = new StringBuilder();
        // Parsed expression
        List<BaseComponent> expr = new ArrayList<>();
        char[] chars = input.toCharArray();
        char cur, next;

        for (int i = 0; i < chars.length; ++i) {
            cur = chars[i];
            next = i < chars.length - 1 ? chars[i + 1] : '\0';

            // Escape special characters
            if (cur == '\\' && (next == COLOR_CHAR || next == FUNCTION_CHAR || next == SECTION_START)) {
                component.append(next);
                ++i;
                continue;
            }

            switch (cur) {
                // Update the format colors in this expression, causes the creation of a new component
                case COLOR_CHAR: {
                    // Finish off the current component if it was started
                    if (component.length() > 0) {
                        expr.add(parseComponent(format, component.toString()));
                        component.setLength(0);
                    }

                    // Get the color arguments
                    Pair<String, Integer> args = getEnclosed(i + 1, input);
                    if (args.getFirst() == null)
                        throw new SyntaxException("Bracket mismatch", i, input);

                    // Move the character index to the end of the arguments
                    i = args.getSecond() - 1;

                    // Parse the color arguments
                    for (String colorText : args.getFirst().split(",")) {
                        // This removes formats
                        boolean negated = colorText.startsWith("!");
                        ChatColor color = Utils.safeValueOf(ChatColor::valueOf, (negated ? colorText.substring(1)
                                : colorText).toUpperCase());

                        if (color == null)
                            throw new SyntaxException("Invalid color code: " + colorText);

                        if (color.isFormat()) {
                            if (negated)
                                format.getSecond().remove(color);
                            else if (!format.getSecond().contains(color))
                                format.getSecond().add(color);
                        } else
                            format.setFirst(color);
                    }

                    continue;
                }

                // Start a new expression
                case SECTION_START: {
                    // Finish off the current component if it was started
                    if (component.length() > 0) {
                        expr.add(parseComponent(format, component.toString()));
                        component.setLength(0);
                    }

                    // Get the expression text
                    Pair<String, Integer> section = getEnclosed(i, input);
                    if (section.getFirst() == null)
                        throw new SyntaxException("Bracket mismatch", i, input);

                    // Move the character index to the end of the expression
                    i = section.getSecond() - 1;

                    // Transfer the current formatting the the next expression in a scope-like manner
                    BaseComponent[] expression = parseExpression(
                            new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                            section.getFirst(),
                            values
                    );
                    expr.addAll(Arrays.asList(expression));

                    continue;
                }

                // Functions
                case FUNCTION_CHAR: {
                    // Get the args
                    Pair<String, Integer> rawArgs = getEnclosed(i + 1, input);
                    if (rawArgs.getFirst() == null)
                        throw new SyntaxException("Bracket mismatch", i, input);
                    List<String> args = new ArrayList<>();

                    // Build the args list
                    StringBuilder currentArg = new StringBuilder();
                    char[] cs = rawArgs.getFirst().toCharArray();
                    int depthCurly = 0, depthRound = 0;
                    for (char c : cs) {
                        // We're in the outermost scope and the delimiter was reached
                        if (depthCurly == 0 && depthRound == 0 && c == ',') {
                            args.add(currentArg.toString());
                            currentArg.setLength(0);
                        } else {
                            // Update scope values
                            switch (c) {
                                case '(':
                                    ++depthRound;
                                    break;
                                case '{':
                                    ++depthCurly;
                                    break;
                                case ')':
                                    --depthRound;
                                    break;
                                case '}':
                                    --depthCurly;
                                    break;
                            }

                            currentArg.append(c);
                        }
                    }

                    // Check for syntax errors
                    if (depthCurly > 0 || depthRound > 0)
                        throw new SyntaxException("Bracket mismatch in function arguments", i, input);

                    // Add the last argument
                    args.add(currentArg.toString());

                    // Move the cursor to the end of the function text
                    i = rawArgs.getSecond() - 1;

                    // Finish off the current component if it was started and we're not inflecting a word
                    if (!"inflect".equalsIgnoreCase(args.get(0)) && component.length() > 0) {
                        expr.add(parseComponent(format, component.toString()));
                        component.setLength(0);
                    }

                    if ("link".equalsIgnoreCase(args.get(0))) { // Args: link, text
                        if (args.size() < 3)
                            throw new SyntaxException("Link function usage: $(link,url,text)");

                        // Parse the text
                        BaseComponent[] text = parseExpression(
                                new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                                args.get(2).startsWith("{") ? getEnclosed(0, args.get(2)).getFirst()
                                        : args.get(2), values
                        );

                        // Apply the link
                        for (BaseComponent bc : text)
                            bc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, args.get(1)));

                        expr.addAll(Arrays.asList(text));
                    } else if ("hover".equalsIgnoreCase(args.get(0))) { // Args: hover text, base text
                        if (args.size() < 3)
                            throw new SyntaxException("Hover function usage: $(hover,hoverText,text)");

                        // Parse both texts
                        BaseComponent[] hover = parseExpression(
                                new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                                args.get(1).startsWith("{") ? getEnclosed(0, args.get(1)).getFirst()
                                        : args.get(1), values
                        );
                        BaseComponent[] text = parseExpression(
                                new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                                args.get(2).startsWith("{") ? getEnclosed(0, args.get(2)).getFirst()
                                        : args.get(2), values
                        );

                        // Apply the hover text
                        for (BaseComponent bc : text)
                            bc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));

                        expr.addAll(Arrays.asList(text));
                    } else if ("inflect".equalsIgnoreCase(args.get(0))) { // Args: value index, word
                        if (args.size() < 4)
                            throw new SyntaxException("Conjugate function usage: $(inflect,noun|verb,argIndex,word)");

                        // Whether or not we're inflecting a noun or verb
                        boolean noun = "noun".equalsIgnoreCase(args.get(1));

                        // Parse the value index
                        int index;
                        try {
                            index = Integer.parseInt(args.get(2));
                        } catch (NumberFormatException ex) {
                            throw new SyntaxException("Invalid index: " + args.get(2));
                        }

                        // Check the index and value
                        if (index > values.length || index < 0)
                            throw new SyntaxException("Index is out of bounds: " + index);
                        if (!(values[index] instanceof Number))
                            throw new SyntaxException("The value at the index provided is not a number.");

                        // Apply the "s" if needed
                        component.append(args.get(3));
                        int count = ((Number) values[index]).intValue();
                        if ((noun && count != 1) || (!noun && count == 1))
                            component.append('s');
                    } else if ("command".equalsIgnoreCase(args.get(0))) { // Args: command, text
                        if (args.size() < 3)
                            throw new SyntaxException("Command function usage: $(command,command,text)");

                        // Parse the text
                        BaseComponent[] text = parseExpression(
                                new Pair<>(format.getFirst(),
                                        new ArrayList<>(format.getSecond())),
                                args.get(2).startsWith("{") ? getEnclosed(0, args.get(2)).getFirst()
                                        : args.get(2), values
                        );

                        // Apply the command
                        for (BaseComponent bc : text)
                            bc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, args.get(1)));

                        expr.addAll(Arrays.asList(text));
                    } else if ("hovercmd".equalsIgnoreCase(args.get(0))) { // Args: command, hover text, base text
                        if (args.size() < 4)
                            throw new SyntaxException("Hover-command function usage: " +
                                    "$(hovercmd,command,hoverText,text)");

                        // Parse both texts
                        BaseComponent[] hover = parseExpression(
                                new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                                args.get(2).startsWith("{") ? getEnclosed(0, args.get(2)).getFirst()
                                        : args.get(2), values
                        );
                        BaseComponent[] text = parseExpression(
                                new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                                args.get(3).startsWith("{") ? getEnclosed(0, args.get(3)).getFirst()
                                        : args.get(3), values
                        );

                        // Apply the command and hover text
                        for (BaseComponent bc : text) {
                            bc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, args.get(1)));
                            bc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
                        }

                        expr.addAll(Arrays.asList(text));
                    } else if ("hoverlink".equalsIgnoreCase(args.get(0))) { // Args: link, hover text, base text
                        if (args.size() < 4)
                            throw new SyntaxException("Hover-link function usage: $(hoverlink,url,hoverText,text)");

                        // Parse both texts
                        BaseComponent[] hover = parseExpression(
                                new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                                args.get(2).startsWith("{") ? getEnclosed(0, args.get(2)).getFirst()
                                        : args.get(2), values
                        );
                        BaseComponent[] text = parseExpression(
                                new Pair<>(format.getFirst(), new ArrayList<>(format.getSecond())),
                                args.get(3).startsWith("{") ? getEnclosed(0, args.get(3)).getFirst()
                                        : args.get(3), values
                        );

                        // Apply the link and hover text
                        for (BaseComponent bc : text) {
                            bc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, args.get(1)));
                            bc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
                        }

                        expr.addAll(Arrays.asList(text));
                    } else
                        throw new SyntaxException("Invalid function: " + args.get(0));

                    continue;
                }

                // Normal text
                default:
                    component.append(cur);
            }

        }

        // Get the last component
        if (component.length() > 0)
            expr.add(parseComponent(format, component.toString()));

        return expr.toArray(new BaseComponent[0]);
    }

    // Create a component with the given color and formatting and text
    private static TextComponent parseComponent(Pair<ChatColor, List<ChatColor>> format, String text) {
        TextComponent tc = new TextComponent(text);

        // Color
        tc.setColor(format.getFirst().asBungee());

        // Formats
        tc.setBold(format.getSecond().contains(ChatColor.BOLD));
        tc.setItalic(format.getSecond().contains(ChatColor.ITALIC));
        tc.setUnderlined(format.getSecond().contains(ChatColor.UNDERLINE));
        tc.setStrikethrough(format.getSecond().contains(ChatColor.STRIKETHROUGH));
        tc.setObfuscated(format.getSecond().contains(ChatColor.MAGIC));

        // Events
        tc.setClickEvent(null);
        tc.setHoverEvent(null);

        return tc;
    }

    // Gets the text enclosed by curved brackets or curly brackets, and returns the text inside the brackets and the
    // index of the character after the last bracket. If the end of the string is encountered before the bracket is
    // closed off, then {null, -1} is returned.
    private static Pair<String, Integer> getEnclosed(int start, String string) {
        boolean curved = string.charAt(start) == '('; // ()s or {}s
        int depth = 1, i = start + 1;
        while (depth > 0) { // Exits when there are no pairs of open brackets
            if (i == string.length()) // Avoid index out of bound errors
                return new Pair<>(null, -1);
            char c = string.charAt(i++);
            if (c == (curved ? ')' : '}')) // We've closed off a pair
                --depth;
            else if (c == (curved ? '(' : '{')) // We've started a pair
                ++depth;
        }
        // Return the stuff inside the brackets, and the index of the char after the last bracket
        return new Pair<>(string.substring(start + 1, i - 1), i);
    }

    /**
     * Represents an exception encountered while parsing the syntax supported by this utility class.
     */
    public static class SyntaxException extends RuntimeException {
        public SyntaxException(String msg, int index, String input) {
            super(msg + " near \"" + input.substring(index, Math.min(index + 8, input.length())) + "\"...");
        }

        public SyntaxException(String msg) {
            super(msg);
        }
    }
}
