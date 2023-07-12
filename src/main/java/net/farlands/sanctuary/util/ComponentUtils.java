package net.farlands.sanctuary.util;

import com.kicas.rp.util.Utils;
import io.papermc.paper.advancement.AdvancementDisplay;
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
import org.bukkit.advancement.Advancement;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
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
     * Create a single component from a list of {@link ComponentLike}s
     * @param list The list to use
     * @param separator The separator between each item of the list, save for the last one
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
     * @param list The list to use
     * @param separator The separator between each item of the list
     * @return The single joined component
     */
    public static Component join(Collection<? extends ComponentLike> list, String separator) {
        return join(list, separator, separator);
    }

    /**
     * Create a single component from a list of {@link ComponentLike}s<br>
     * Separator: ", "<br>
     * Final Separator: " and " if the list is only two long, or ", and " if it's longer.
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
     * _If {@code values} is empty, then it immediately returns a component of the format.  This means that no brackets are formatted._
     * <p>
     * This formats using a bracket notation.  See the examples.
     * <p>
     * Examples:
     * _Note: these examples show it returning strings because we can't show components in a comment_
     *
     * <pre>
     * The {} notation can be used to insert the next value provided
     * format("{}", "hello") -> "hello"
     * format("Hello {}!", "world") -> "Hello world!"
     * format("Hello {}!", "world") -> "Hello world!"
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
     * The function accepts _any_ arguments, default to using {@link String#valueOf} if it's not in the select items listed in {@link ComponentUtils#toComponent}.
     * format("{}", 5000) -> "5000"
     *
     * Setting the stringFormat to 's' makes is use an s if the number != 1
     * format("{} point{0::s}", 5) -> "5 points"
     * format("{} point{0::s}", 1) -> "1 point"
     *
     * Java {@link String#format} syntax can be used for values
     * format("{::%,d}", 5000) -> "5,000"
     * </pre>
     *
     * ## Custom formatting
     *
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
                    current = new StringBuilder();
                }
                case '}' -> {
                    if (!inBlock) {
                        current.append('}');
                        continue;
                    }

                    var index = new AtomicInteger(valueIndex);
                    builder.append(parseInner(current.toString(), values, index));
                    valueIndex = index.get();
                    inBlock = false;
                    current = new StringBuilder();
                }
                case '\\' -> escaping = true;
                default -> current.append(c);
            }
        }
        builder.append(Component.text(current.toString()));
        return builder.build();
    }

    /**
     * Parse the inside portion of the {@code {}} for {@link ComponentUtils#format}.
     * <p>
     * Syntax:
     * {@code [index][[:][format][:stringFormat]]}
     * <p>
     * <p>
     * <p>
     * But that's confusing, so let's do some examples
     * <p>
     * ({@code index = auto} means that index will use the next {@code valueIndex}, {@code format = null} means it will inherit from parent, and {@code stringFormat = null} means that it will use {@link String#valueOf})
     * <pre>
     * {@code {}                 } -> {@code index = auto, format = null,          stringFormat = null }
     * {@code {0}                } -> {@code index = 0,    format = null,          stringFormat = null }
     * {@code {0:red}            } -> {@code index = 0,    format = red,           stringFormat = null }
     * {@code {0:red bold}       } -> {@code index = 0,    format = red & bold,    stringFormat = null }
     * {@code {0:red green bold} } -> {@code index = 0,    format = green & bold,  stringFormat = null }
     * {@code {0:red !bold}      } -> {@code index = 0,    format = red & !bold,   stringFormat = null }
     * {@code {0:red:%,d}        } -> {@code index = 0,    format = red,           stringFormat = %,d  }
     * {@code {:red:%,d}         } -> {@code index = auto, format = red,           stringFormat = %,d  }
     * {@code {::%,d}            } -> {@code index = auto, format = null,          stringFormat = %,d  }
     * {@code {::}               } -> {@code index = auto, format = null,          stringFormat = null }
     * {@code {red}              } -> {@code index = auto, format = red,           stringFormat = null }
     * {@code {red bold}         } -> {@code index = auto, format = red & bold,    stringFormat = null }
     * {@code {red:}             } -> error
     * {@code {:red:}            } -> {@code index = auto, format = red,           stringFormat = null }
     * {@code {:red:a}           } -> {@code index = auto, format = red,           stringFormat = a    }
     * {@code {asdf}             } -> error
     * {@code {:asdf}            } -> error
     * {@code {:asdf:}           } -> error
     * {@code {:asdf:}           } -> error
     * </pre>
     * @param value
     * @param objs
     * @param valueIndex
     * @return
     */
    private static Component parseInner(String value, Object[] objs, AtomicInteger valueIndex) {
        int index;
        String format = null;
        String stringFormat = null;

        int colPos = value.indexOf(':');

        // Format: [index][[:]format[:stringFormat]]
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

            format = value.substring(colPos + 1); // Get the format: "bbbb:cccc"
            int formatColPos = format.indexOf(':');
            if (formatColPos != -1) { // We have "bbbb:cccc"
                stringFormat = format.substring(formatColPos + 1); // "cccc"
                format = format.substring(0, formatColPos); // "bbbb"
            }
        } else { // We have {aaaa} or {}
            try {
                if (value.isBlank()) { // We have {}
                    index = valueIndex.getAndIncrement();
                } else { // We have {aaaa}
                    index = Integer.parseInt(value); // Get the value
                    if (index == valueIndex.get()) { // If we have "{0} {1} {2}", increment valueIndex
                        valueIndex.getAndIncrement();
                    }
                }
            } catch (NumberFormatException ex) { // If it's not an int, they probably want the formatting, so let's treat it as {:bbbb}
                index = valueIndex.getAndIncrement();
                format = value;
            }

        }

        var obj = objs[index];

        var style = Component.empty();
        if (format != null && !format.isBlank()) {
            String[] formats = format.split("[^\\w#!]+"); // Don't just split on ' ' so that we're lenient about what we accept: "a,b,c" == "a b c"
            for (var fmt : formats) {
                if (fmt.startsWith("#")) { // Hex colour value
                    var col = TextColor.fromCSSHexString(fmt);
                    if (col == null) {
                        throw new IllegalArgumentException("Invalid Format: " + fmt);
                    }
                    style = style.color(col);
                } else { // Either a colour of a style
                    var col = NamedTextColor.NAMES.value(fmt.toLowerCase());
                    if (col == null) { // Not a colour
                        boolean negate = fmt.startsWith("!");

                        if (negate) {
                            fmt = fmt.substring(1);
                        }

                        var td = Utils.valueOfFormattedName(fmt, TextDecoration.class);
                        if (td == null) { // Not a decoration and has no other options
                            throw new IllegalArgumentException("Invalid Format: " + fmt);
                        }
                        style = style.decoration(td, !negate);
                    } else { // It is a colour
                        style = style.color(col);
                    }
                }
            }
        }

        // Convert the object to a component and add style
        Component o = toComponent(obj, stringFormat);
        return o.mergeStyle(style);
    }

    /**
     * Convert an object to Component in a custom way that looks pretty nice.
     * <p>
     * This function will accept any object and format it in a way that looks decent.
     * This is done by checking if the object is of a few key types and creating components based on that.
     * <p>
     * <p>
     * <p>
     * The list of current checks and what they do (ordered by priority):
     * <ul>
     * <li>{@link ComponentLike} -> {@link ComponentLike#asComponent}</li>
     * <li>{@link Enum} -> {@link Utils#formattedName}</li>
     * <li>{@link ItemStack} -> {@link ComponentUtils#item(ItemStack)}</li>
     * <li>{@link Advancement} -> [title]</li>
     * <li>{@link Integer} and stringFormat == s -> "s" if int == 1 else ""</li>
     * <li>{@link Collection} -> toComponent(col[0]), ... -> a, b, c, and d</li>
     * <li>{@link Collection} if stringFormat == "[]" -> [toComponent(col[0]), ...] -> [a, b, c, d]</li>
     * <li>{@code Object[]} -> toComponent(col[0]) ... -> a, b, c, and d</li>
     * <li>{@code stringFormat != null} -> {@link String#format}</li>
     * <li>{@code _} -> {@link String#valueOf}</li>
     * </ul>
     * @param obj
     * @param stringFormat
     * @return
     */
    private static Component toComponent(Object obj, String stringFormat) {
        Component o;
        boolean brackets = stringFormat != null && stringFormat.startsWith("[") && stringFormat.endsWith("]");
        if (obj instanceof ComponentLike c) {
            o = c.asComponent();
        } else if (obj instanceof Enum<?> e) {
            o = Component.text(Utils.formattedName(e));
        } else if (obj instanceof ItemStack is) {
            o = item(is);
        } else if (obj instanceof Advancement adv) {
            AdvancementDisplay advDisplay = adv.getDisplay();
            o = ComponentUtils.hover(
                ComponentColor.color(advDisplay.frame().color(), "[{}]", advDisplay.title()),
                ComponentColor.color(advDisplay.frame().color(), "{}\n{}", advDisplay.title(), advDisplay.description())
            );
        } else if (obj instanceof Integer n && stringFormat != null && stringFormat.equalsIgnoreCase("s")) {
            o = n == 1 ? Component.empty() : Component.text("s");
        } else if (obj instanceof Collection<?> coll) {
            var b = Component.text();
            if (brackets) {
                b.content("[");
            }
            var iter = coll.iterator();
            var size = coll.size();

            for (int i = 0; iter.hasNext(); ++i) {
                var n = iter.next();
                b.append(toComponent(n, brackets ? stringFormat.substring(1, stringFormat.length() - 1) : stringFormat));

                if (size > 2) {
                    if (i < size - 1) {
                        b.append(Component.text(i == size - 2 && !brackets ? ", and " : ", "));
                    }
                } else if (size == 2 && i == 0 && !brackets) {
                    b.append(Component.text(" and "));
                }
            }
            if (brackets) {
                b.append(Component.text("]"));
            }
            o = b.build();
        } else if (obj instanceof Object[] arr) {
            var b = Component.text();
            if (brackets) {
                b.content("[");
            }
            for (int i = 0; i < arr.length; ++i) {
                var n = arr[i];
                b.append(toComponent(n, brackets ? stringFormat.substring(1, stringFormat.length() - 1) : stringFormat));

                if (arr.length > 2) {
                    if (i < arr.length - 1) {
                        b.append(Component.text(i == arr.length - 2 && !brackets ? ", and " : ", "));
                    }
                } else if (arr.length == 2 && i == 0 && !brackets) {
                        b.append(Component.text(" and "));
                }
            }
            if (brackets) {
                b.append(Component.text("]"));
            }
            o = b.build();
        } else {
            String s;
            if (stringFormat != null && !stringFormat.isBlank()) { // stringFormat is set
                s = String.format(stringFormat, obj);
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
