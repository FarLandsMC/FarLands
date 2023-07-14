package net.farlands.sanctuary.util;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.chat.MiniMessageWrapper;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.advancement.Advancement;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for creating/manipulating components
 */
public class ComponentUtils {

    /**
     * Create a component with the actions for a command
     *
     * @param command The command to run
     * @param base    The base component
     * @param hover   The hover component
     * @return The complete component
     */
    public static Component command(String command, Component base, Component hover) {
        return base.clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(hover));
    }

    /**
     * Create a component with the styling and actions for a command
     *
     * @param command The command
     * @param color   The color for the text
     * @return Command Component
     */
    public static Component command(String command, TextColor color) {
        Component c = Component.text(command).color(color);
        return command(command, c);
    }

    /**
     * Create a component with the styling and actions for a command
     *
     * @param command The command
     * @param base    The base component
     */
    public static Component command(String command, Component base) {
        return command(command, base, ComponentColor.gray("Run Command"));
    }

    /**
     * Create a component with the styling and actions for a command with aqua color
     *
     * @param command The command
     * @return Command Component
     */
    public static Component command(String command) {
        return command(command, NamedTextColor.AQUA);
    }

    /**
     * Create a component with the actions to suggest a command
     *
     * @param fillCommand The command to fill with
     * @param base        Base Component
     * @param hover       Hover Component
     * @return The full component
     */
    public static Component suggestCommand(String fillCommand, Component base, Component hover) {
        return base.clickEvent(ClickEvent.suggestCommand(fillCommand))
            .hoverEvent(HoverEvent.showText(hover));
    }

    /**
     * Create a component with the styling and actions to suggest a command
     *
     * @param fillCommand The command to fill with
     * @param base        Base Component
     * @return The full component
     */
    public static Component suggestCommand(String fillCommand, Component base) {
        return suggestCommand(fillCommand, base, ComponentColor.gray("Fill Command"));
    }

    /**
     * Create a component with the styling and actions to suggest a command
     *
     * @param command     The command to show
     * @param fillCommand The command to fill with
     * @param color       The color of the base text
     * @return The full component
     */
    public static Component suggestCommand(String command, String fillCommand, TextColor color) {
        Component c = Component.text(command).color(color);
        return suggestCommand(fillCommand, c);
    }


    /**
     * Create a component with the styling and actions to suggest a command
     *
     * @param command     The command to show
     * @param fillCommand The command to fill with
     * @return The full component
     */
    public static Component suggestCommand(String command, String fillCommand) {
        return suggestCommand(command, fillCommand, NamedTextColor.AQUA);
    }


    /**
     * Create a component with the styling and actions to suggest a command
     *
     * @param command The command to show/fill
     * @return The full component
     */
    public static Component suggestCommand(String command) {
        return suggestCommand(command, command, NamedTextColor.AQUA);
    }

    /**
     * Create a component with the styling and actions for a link
     *
     * @param baseText The base text of the link
     * @param url      The link
     * @param color    The color for the link
     * @return Link Component
     */
    public static Component link(String baseText, String url, TextColor color) {
        Component c = Component.text(baseText).style(Style.style(color, TextDecoration.UNDERLINED));
        return c.clickEvent(ClickEvent.openUrl(url))
            .hoverEvent(HoverEvent.showText(ComponentColor.gray("Open Link")));
    }

    /**
     * Create a component with the styling and actions for a link
     *
     * @param baseText The base text of the link
     * @param url      The link
     * @return Link Component
     */
    public static Component link(String baseText, String url) {
        return link(baseText, url, NamedTextColor.AQUA);
    }

    /**
     * Create a component with the styling and actions for a link
     *
     * @param url The link
     * @return Link Component
     */
    public static Component link(String url) {
        return link(url, url);
    }

    /**
     * Create a component that displays an item on hover
     *
     * @param base The base component, without the hover event
     * @param item The item to display
     * @return Item Component
     */
    public static Component item(Component base, ItemStack item) {
        return base.hoverEvent(item.asHoverEvent());
    }


    /**
     * Create a component that displays an item on hover
     *
     * @param baseText The base text
     * @param item     The item to display
     * @return Item Component
     */
    public static Component item(String baseText, ItemStack item) {
        return item(Component.text(baseText), item);
    }

    /**
     * Create a component that displays an item on hover
     *
     * @param item The item to show
     * @return Item Component
     */
    public static Component item(ItemStack item) {
        return item(ComponentColor.aqua(item.getAmount() > 1 ? item.getAmount() + " * " : "").append(item.displayName()), item);
    }

    /**
     * Create a hover text on a component
     *
     * @param base  The base component
     * @param hover The hover component
     * @return The complete component
     */
    public static Component hover(Component base, Component hover) {
        return base.hoverEvent(HoverEvent.showText(hover));
    }

    /**
     * Create a hover text on a component
     *
     * @param base      The base component
     * @param hoverText The hover text
     * @return The complete component
     */
    public static Component hover(Component base, String hoverText) {
        return hover(base, Component.text(hoverText));
    }

    /**
     * Create component from an advancement
     *
     * @param adv The advancement
     * @return The complete component
     */
    public static Component advancement(Advancement adv) {
        var d = adv.getDisplay();
        return hover(
            ComponentColor.color(d.frame().color(), "[{}]", d.title()),
            ComponentColor.color(d.frame().color(), "{}\n{}", d.title(), d.description())
        );
    }

    /**
     * Create a single component from a list of {@link ComponentLike}s
     *
     * @param list           The list to use
     * @param separator      The separator between each item of the list, save for the last one
     * @param finalSeparator The separator between the final two items of the list, commonly used for ", and "
     * @return The single joined component
     */
    public static Component join(Collection<? extends ComponentLike> list, String separator, String finalSeparator) {
        return Component.join(
            JoinConfiguration.separators(
                Component.text(separator),
                Component.text(finalSeparator)
            ),
            list
        );
    }

    /**
     * Create a single component from a list of {@link ComponentLike}s
     *
     * @param list      The list to use
     * @param separator The separator between each item of the list
     * @return The single joined component
     */
    public static Component join(Collection<? extends ComponentLike> list, String separator) {
        return join(list, separator, separator);
    }

    /**
     * Create a single component from a list of {@link ComponentLike}s<br> Separator: ", "<br> Final Separator: " and "
     * if the list is only two long, or ", and " if it's longer.
     *
     * @param list The list to use
     * @return The single joined component
     */
    public static Component join(Collection<? extends ComponentLike> list) {
        return join(list, ", ", list.size() == 2 ? " and " : ", and ");
    }

    /**
     * Convert from {@link Component}s into plain text
     *
     * @param component Components to convert
     * @return text
     */
    public static String toText(Component component) {
        return PlainTextComponentSerializer.plainText().serializeOr(component, "");
    }

    /**
     * Convert from a {@link TextColor} into {@link Color}
     *
     * @param color The color to convert from
     * @return The converted color
     */
    public static Color getColor(TextColor color) {
        return new Color(color.value());
    }

    /**
     * Parse a string using MiniMessage or Legacy into components, depending on flp's permissions
     * <p>
     * (Adept+ and staff)
     *
     * @param message The message to parse
     * @param flp     The sender of the message
     */
    public static Component parse(String message, OfflineFLPlayer flp) {
        return MiniMessageWrapper.farlands(flp).mmParse(message);
    }

    /**
     * Parse a string using MiniMessage or Legacy into components using all features
     *
     * @param message The message to parse
     */
    public static Component parse(String message) {
        return MiniMessageWrapper.builder()
            .gradients(true)
            .hexColors(true)
            .standardColors(true)
            .legacyColors(true)
            .advancedTransformations(true)
            .preventLuminanceBelow(0)
            .build()
            .mmParse(message);
    }

    /**
     * Remove hover and click events from the supplied ComponentLike
     *
     * @param componentLike Source
     * @return Source without events
     */
    public static Component noEvent(ComponentLike componentLike) {
        return componentLike.asComponent().hoverEvent(null).clickEvent(null);
    }

    /**
     * Create component from a given format and a list of objects.
     * <p>
     * _If {@code values} is empty, then it immediately returns a component of the format.  This means that no brackets
     * are formatted._
     * <p>
     * This formats using a bracket notation.  See the examples.
     * <p>
     * Examples: _Note: these examples show it returning strings because we can't show components in a comment_
     *
     * <pre>
     * The {} notation can be used to insert the next value provided
     * format("{}", "hello") -> "hello"
     * format("Hello {}!", "world") -> "Hello world!"
     * format("Hello {} and {}!", "world", "earth") -> "Hello world!"
     *
     *
     * The {NUMBER} notation can be used to insert a value from the provided index
     * format("Hello {} {0}!", "world") -> "Hello world world!"
     *
     * Formatting can be applied by using the name of the colour/decoration with a space in between
     * format("Hello {:red}!", "world") -> "Hello world!" (world is red)
     * format("Hello {} {0:red}!", "world") -> "Hello world world!" (the second "world" is red)
     * format("Hello {:red bold}!", "world") -> "Hello world!" ("world" is red and bold)
     *
     * Formatting can be negated from the parent component
     * format("Hello {:red !bold}!", "world") -> "Hello world!" ("world" is red and explicitly not bold)
     *
     * The function accepts _any_ arguments, see {@link ComponentUtils#toComponent(Object, String)} for formatting rules
     * format("{}", 5000) -> "5000"
     *
     * The third value is given as a custom format that the {@link ComponentUtils#toComponent(Object, String)} method takes advantage of
     * format("{::%,d}", 5000) -> "5,000"
     * format("{0} num{0::s}", 5) -> " 5 nums"
     * format("{::[]}", List.of(1, 2, 3)) -> "[1, 2, 3]"
     * format("{::[%02d]}", List.of(1, 2, 3)) -> "[01, 02, 03]"
     *
     * The {} can be escpaed with a \ in front of the symbol
     * format("Hello \{}!", "world") -> "Hello {}!"
     * format("Hello {::\}}!", "world") -> "Hello }!"
     *
     * Stray { inside a block will be auto escaped
     * format("Hello {::{}!", "world") -> "Hello {!"
     *
     * Setting the stringFormat to 's' makes is use an s if the number != 1
     * format("{} point{0::s}", 5) -> "5 points"
     * format("{} point{0::s}", 1) -> "1 point"
     *
     * If the arg is a bool, setting the stringFormat to be `?a:b` -> bool ? "a" : "b"
     * (See {@link net.farlands.sanctuary.command.player.CommandPvP} for usage example.)
     * format("Hello {::?world:earth}", true)  -> "Hello world"
     * format("Hello {::?world:earth}", false) -> "Hello earth"
     *
     * </pre>
     * <p>
     * ## Custom formatting
     * <p>
     * See {@link ComponentUtils#toComponent} for the list of custom formats.
     *
     * @param format The format to use
     * @param values The values to use as replacements
     * @return The formatted component
     */
    public static Component format(String format, Object... values) {
        if (values.length == 0) {
            return Component.text(format);
        }

        StringBuilder current = new StringBuilder();
        int valueIndex = 0;
        boolean escaping = false;
        boolean inBlock = false;
        var builder = Component.text();

        for (char c : format.toCharArray()) {
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }

            switch (c) {
                case '{' -> {
                    if (inBlock) {
                        current.append('{');
                        continue;
                    }
                    builder.append(Component.text(current.toString()));
                    inBlock = true;
                    current.setLength(0);
                }
                case '}' -> {
                    if (!inBlock) {
                        current.append('}');
                        continue;
                    }

                    var index = new AtomicInteger(valueIndex); // This allows the `parseInner` fn to mutate index if needed
                    builder.append(parseInner(current.toString(), values, index));
                    valueIndex = index.get();

                    inBlock = false;
                    current.setLength(0);
                }
                case '\\' -> escaping = true;
                default -> current.append(c);
            }
        }
        if (inBlock) {
            current.insert(0, '{');
        }
        builder.append(Component.text(current.toString()));
        return builder.build();
    }

    /**
     * Parse the inside portion of the {@code {}} for {@link ComponentUtils#format}.
     * <p>
     * Syntax: {@code [index][[:][format][:stringFormat]]}
     * <p>
     * <p>
     * <p>
     * But that's confusing, so let's do some examples
     * <p>
     * ({@code index = auto} means that index will use the next {@code valueIndex}, {@code style = null} means it will
     * inherit from parent, and {@code fmt = null} means that it will use {@link String#valueOf})
     * <pre>{@code
     * {}                  -> index = auto, style = null,         fmt = null
     * {0}                 -> index = 0,    style = null,         fmt = null // If valueIndex == 0, it will be incremented
     * {5}                 -> index = 0,    style = null,         fmt = null // If valueIndex == 5, it will be incremented
     * {0:red}             -> index = 0,    style = red,          fmt = null // If valueIndex == 0, it will be incremented
     * {0:red bold}        -> index = 0,    style = red & bold,   fmt = null // If valueIndex == 0, it will be incremented
     * {0:red green bold}  -> index = 0,    style = green & bold, fmt = null // If valueIndex == 0, it will be incremented
     * {0:red !bold}       -> index = 0,    style = red & !bold,  fmt = null // If valueIndex == 0, it will be incremented
     * {0:red:%,d}         -> index = 0,    style = red,          fmt = %,d  // If valueIndex == 0, it will be incremented
     * {:red:%,d}          -> index = auto, style = red,          fmt = %,d
     * {::%,d}             -> index = auto, style = null,         fmt = %,d
     * {::}                -> index = auto, style = null,         fmt = null
     * {asdf:}             -> error
     * {asdf:asdf:asdf}    -> error
     * {:red:}             -> index = auto, style = red,          fmt = null
     * {:red:asdf}         -> index = auto, style = red,          fmt = asdf
     * {:red:as:df}        -> index = auto, style = red,          fmt = as:df
     * {asdf}              -> error
     * {:asdf}             -> error
     * {:asdf:}            -> error
     * {:asdf:}            -> error
     * }</pre>
     *
     * @param value      The value to parse (excluding the `{}`)
     * @param objs       The objects to use
     * @param valueIndex The next index to use -- this can be mutated
     * @return
     */
    private static Component parseInner(String value, Object[] objs, AtomicInteger valueIndex) {
        int index;
        String styleStr = null;
        String fmt = null;

        int colPos = value.indexOf(':');

        // Format: [index][[:]styleStr[:fmt]]
        if (colPos != -1) { // We have {aaaa:bbbb} or {aaaa:bbbb:cccc} or {:bbbb:cccc}
            var indexStr = value.substring(0, colPos);
            if (indexStr.isBlank()) { // We have {:bbbb} or {:cccc}
                index = valueIndex.getAndIncrement(); // Get the next value
            } else { // Get the value of the int
                index = Integer.parseInt(indexStr);
                if (index == valueIndex.get()) { // If we have "{0} {1} {2}", increment valueIndex
                    valueIndex.getAndIncrement();
                }
            }

            styleStr = value.substring(colPos + 1); // Get the styleStr: "bbbb:cccc"
            int col2 = styleStr.indexOf(':');
            if (col2 != -1) { // We have "bbbb:cccc"
                fmt = styleStr.substring(col2 + 1); // "cccc"
                styleStr = styleStr.substring(0, col2); // "bbbb"
            }
        } else { // We have {aaaa} or {}
            if (value.isBlank()) { // We have {}
                index = valueIndex.getAndIncrement();
            } else { // We have {aaaa}
                index = Integer.parseInt(value); // Get the value
                if (index == valueIndex.get()) { // If we have "{0} {1} {2}", increment valueIndex
                    valueIndex.getAndIncrement();
                }
            }
        }

        var obj = objs[index];
        Component o = toComponent(obj, fmt);

        if (styleStr != null && !styleStr.isBlank()) {
            String[] styles = styleStr.split("[^\\w#!]+"); // Don't just split on ' ' so that we're lenient about what we accept: "a,b,c" == "a b c"
            for (var s : styles) {
                if (s.startsWith("#")) { // Hex colour value
                    var col = TextColor.fromCSSHexString(s);
                    if (col == null) {
                        throw new IllegalArgumentException("Invalid Format: " + s);
                    }
                    o = o.color(col);
                } else { // Either a colour of a style
                    var col = NamedTextColor.NAMES.value(s.toLowerCase().replace('_', '-'));
                    if (col != null) { // It is a colour
                        o = o.color(col);
                    } else { // Not a colour
                        boolean negate = s.startsWith("!");

                        if (negate) {
                            s = s.substring(1);
                        }

                        var td = Utils.valueOfFormattedName(s, TextDecoration.class);
                        if (td == null) { // Not a decoration and has no other options
                            throw new IllegalArgumentException("Invalid Format: " + s);
                        }
                        o = o.decoration(td, !negate);
                    }
                }
            }
        }

        // Convert the object to a component and add style
        return o.mergeStyle(o);
    }

    /**
     * Convert an object to Component in a custom way that looks pretty nice.
     * <p>
     * This function will accept any object and format it in a way that looks decent. This is done by checking if the
     * object is of a few key types and creating components based on that.
     * <p>
     * <p>
     * <p>
     * The list of current checks and what they do (ordered by priority):
     * <pre>{@code
     * ComponentLike                   -> ComponentLike#asComponent
     * Enum                            -> Utils#formattedName
     * ItemStack                       -> ComponentUtils#item(ItemStack)
     * Advancement                     -> ComponentUtils#advancement(Advancement)
     * Translatable   if fmt           -> Component#translatable(Translatable, String) with fmt as fallback
     * Translatable                    -> Component#translatable(Translatable)
     * Integer        if fmt == s      -> n == 1 ? "s" : ""
     * Boolean        if fmt ~= "?a:b" -> bool   ? "a" : "b" (note: "?a:b:c" -> "a" if bool else "b:c")
     * Collection                      -> a, b, c, and d (via toComponent) fm passed down fmt = "%02d" -> 01, 02, and 03
     * Collection     if fmt == "[]"   -> [a, b, c, d]   (via toComponent) brackets inner fmt passed down (fmt = "[%02d]" -> [01, 02, 03])
     * Object[]                        -> a, b, c, and d (via toComponent) ^^^
     * Object[]       if fmt == "[]"   -> [a, b, c, d]   (via toComponent) ^^^
     * _              if fmt != null   -> String#format
     * _                               -> String#valueOf
     * }</pre>
     *
     * @param obj The object to convert -- may be anything
     * @param fmt The format to use, optional
     * @return The formatted component
     */
    private static Component toComponent(@Nullable Object obj, @Nullable String fmt) {
        Component o;
        boolean brackets = fmt != null && fmt.startsWith("[") && fmt.endsWith("]");
        if (obj instanceof ComponentLike c) {
            o = c.asComponent();

        } else if (obj instanceof Enum<?> e) {
            o = Component.text(Utils.formattedName(e));

        } else if (obj instanceof ItemStack is) {
            o = item(is);

        } else if (obj instanceof Advancement adv) {
            o = ComponentUtils.advancement(adv);

        } else if (obj instanceof Translatable t) {
            if (fmt == null || fmt.isBlank()) {
                o = Component.translatable(t);
            } else {
                o = Component.translatable(t, fmt);
            }
        } else if (obj instanceof Integer n && fmt != null && fmt.equalsIgnoreCase("s")) {
            o = Component.text(n == 1 ? "" : "s");

        } else if (obj instanceof Boolean b && fmt != null && fmt.indexOf('?') == 0 && fmt.indexOf(':') != -1) {
            int c = fmt.indexOf(':');
            o = Component.text(b ? fmt.substring(1, c) : fmt.substring(c + 1));
        } else if (obj instanceof Collection<?> coll && brackets) {
            var inner = fmt.substring(1, fmt.length() - 1);
            o = Component.join( // [a, b, c]
                                JoinConfiguration.arrayLike(),
                                coll.stream().map(c -> ComponentUtils.toComponent(c, inner)).toList()
            );

        } else if (obj instanceof Object[] arr && brackets) {
            var inner = fmt.substring(1, fmt.length() - 1);
            o = Component.join( // [a, b, c]
                                JoinConfiguration.arrayLike(),
                                Arrays.stream(arr).map(c -> ComponentUtils.toComponent(c, inner)).toList()
            );

        } else if (obj instanceof Collection<?> coll) {
            var size = coll.size();
            var iter = coll.iterator();
            o = switch (size) {
                case 1 -> toComponent(iter.next(), fmt);   // a
                case 2 -> Component.text()                 // a and b
                    .append(toComponent(iter.next(), fmt))
                    .append(Component.text(" and "))
                    .append(toComponent(iter.next(), fmt))
                    .build();
                default -> {                               // a, b, ..., and z
                    var b = Component.text();
                    for (int i = 0; iter.hasNext(); ++i) {
                        b.append(toComponent(iter.next(), fmt));
                        if (i < size - 1) {
                            b.append(Component.text(i == size - 2 ? ", and " : ", "));
                        }
                    }
                    yield b.build();
                }
            };

        } else if (obj instanceof Object[] arr) {
            var size = arr.length;
            o = switch (size) {
                case 1 -> toComponent(arr[0], fmt);   // a
                case 2 -> Component.text()            // a and b
                    .append(toComponent(arr[0], fmt))
                    .append(Component.text(" and "))
                    .append(toComponent(arr[1], fmt))
                    .build();
                default -> {                          // a, b, ..., and z
                    var b = Component.text();
                    for (int i = 0; i < arr.length; ++i) {
                        b.append(toComponent(arr[i], fmt));
                        if (i < size - 1) {
                            b.append(Component.text(i == size - 2 ? ", and " : ", "));
                        }
                    }
                    yield b.build();
                }
            };

        } else {
            String s;
            if (fmt != null && !fmt.isBlank()) { // fmt is set
                s = String.format(fmt, obj);
            } else { // it's unset
                s = String.valueOf(obj);
            }
            o = Component.text(s);

        }
        return o;
    }

    /**
     * Generate a component from an object using {@link String#valueOf}
     */
    public static Component format(Object obj) {
        return format("{}", obj);
    }
}
