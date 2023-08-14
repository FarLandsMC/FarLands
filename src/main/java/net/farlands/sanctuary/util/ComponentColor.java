package net.farlands.sanctuary.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Contract;

import static net.farlands.sanctuary.util.ComponentUtils.format;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class ComponentColor {

    // Note: All methods are annotated with `@Contract(pure = true)` so IDEs warn about unused results (same effect as @CheckReturnValue)

    /**
     * Generate a component with the colour of BLACK.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component black(String format, Object... values) {
        return format(format, values).color(BLACK);
    }

    /**
     * Generate a component with the colour of BLACK using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component black(Object obj) {
        return black("{}", obj);
    }

    /**
     * Generate a component with the colour of BLACK
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component black(String str) {
        return Component.text(str).color(BLACK);
    }

    /**
     * Wrap a component with the colour of BLACK
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component black(Component comp) {
        return Component.empty().append(comp).color(BLACK);
    }

    /**
     * Generate a component with the colour of DARK_BLUE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component darkBlue(String format, Object... values) {
        return format(format, values).color(DARK_BLUE);
    }

    /**
     * Generate a component with the colour of DARK_BLUE using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component darkBlue(Object obj) {
        return darkBlue("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_BLUE
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component darkBlue(String str) {
        return Component.text(str).color(DARK_BLUE);
    }

    /**
     * Wrap a component with the colour of DARK_BLUE
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component darkBlue(Component comp) {
        return Component.empty().append(comp).color(DARK_BLUE);
    }

    /**
     * Generate a component with the colour of DARK_GREEN.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component darkGreen(String format, Object... values) {
        return format(format, values).color(DARK_GREEN);
    }

    /**
     * Generate a component with the colour of DARK_GREEN using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component darkGreen(Object obj) {
        return darkGreen("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_GREEN
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component darkGreen(String str) {
        return Component.text(str).color(DARK_GREEN);
    }

    /**
     * Wrap a component with the colour of DARK_GREEN
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component darkGreen(Component comp) {
        return Component.empty().append(comp).color(DARK_GREEN);
    }

    /**
     * Generate a component with the colour of DARK_AQUA.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component darkAqua(String format, Object... values) {
        return format(format, values).color(DARK_AQUA);
    }

    /**
     * Generate a component with the colour of DARK_AQUA using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component darkAqua(Object obj) {
        return darkAqua("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_AQUA
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component darkAqua(String str) {
        return Component.text(str).color(DARK_AQUA);
    }

    /**
     * Wrap a component with the colour of DARK_AQUA
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component darkAqua(Component comp) {
        return Component.empty().append(comp).color(DARK_AQUA);
    }

    /**
     * Generate a component with the colour of DARK_RED.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component darkRed(String format, Object... values) {
        return format(format, values).color(DARK_RED);
    }

    /**
     * Generate a component with the colour of DARK_RED using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component darkRed(Object obj) {
        return darkRed("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_RED
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component darkRed(String str) {
        return Component.text(str).color(DARK_RED);
    }

    /**
     * Wrap a component with the colour of DARK_RED
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component darkRed(Component comp) {
        return Component.empty().append(comp).color(DARK_RED);
    }

    /**
     * Generate a component with the colour of DARK_PURPLE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component darkPurple(String format, Object... values) {
        return format(format, values).color(DARK_PURPLE);
    }

    /**
     * Generate a component with the colour of DARK_PURPLE using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component darkPurple(Object obj) {
        return darkPurple("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_PURPLE
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component darkPurple(String str) {
        return Component.text(str).color(DARK_PURPLE);
    }

    /**
     * Wrap a component with the colour of DARK_PURPLE
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component darkPurple(Component comp) {
        return Component.empty().append(comp).color(DARK_PURPLE);
    }

    /**
     * Generate a component with the colour of GOLD.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component gold(String format, Object... values) {
        return format(format, values).color(GOLD);
    }

    /**
     * Generate a component with the colour of GOLD using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component gold(Object obj) {
        return gold("{}", obj);
    }

    /**
     * Generate a component with the colour of GOLD
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component gold(String str) {
        return Component.text(str).color(GOLD);
    }

    /**
     * Wrap a component with the colour of GOLD
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component gold(Component comp) {
        return Component.empty().append(comp).color(GOLD);
    }

    /**
     * Generate a component with the colour of GRAY.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component gray(String format, Object... values) {
        return format(format, values).color(GRAY);
    }

    /**
     * Generate a component with the colour of GRAY using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component gray(Object obj) {
        return gray("{}", obj);
    }

    /**
     * Generate a component with the colour of GRAY
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component gray(String str) {
        return Component.text(str).color(GRAY);
    }

    /**
     * Wrap a component with the colour of GRAY
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component gray(Component comp) {
        return Component.empty().append(comp).color(GRAY);
    }

    /**
     * Generate a component with the colour of DARK_GRAY.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component darkGray(String format, Object... values) {
        return format(format, values).color(DARK_GRAY);
    }

    /**
     * Generate a component with the colour of DARK_GRAY using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component darkGray(Object obj) {
        return darkGray("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_GRAY
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component darkGray(String str) {
        return Component.text(str).color(DARK_GRAY);
    }

    /**
     * Wrap a component with the colour of DARK_GRAY
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component darkGray(Component comp) {
        return Component.empty().append(comp).color(DARK_GRAY);
    }

    /**
     * Generate a component with the colour of BLUE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component blue(String format, Object... values) {
        return format(format, values).color(BLUE);
    }

    /**
     * Generate a component with the colour of BLUE using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component blue(Object obj) {
        return blue("{}", obj);
    }

    /**
     * Generate a component with the colour of BLUE
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component blue(String str) {
        return Component.text(str).color(BLUE);
    }

    /**
     * Wrap a component with the colour of BLUE
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component blue(Component comp) {
        return Component.empty().append(comp).color(BLUE);
    }

    /**
     * Generate a component with the colour of GREEN.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component green(String format, Object... values) {
        return format(format, values).color(GREEN);
    }

    /**
     * Generate a component with the colour of GREEN using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component green(Object obj) {
        return green("{}", obj);
    }

    /**
     * Generate a component with the colour of GREEN
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component green(String str) {
        return Component.text(str).color(GREEN);
    }

    /**
     * Wrap a component with the colour of GREEN
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component green(Component comp) {
        return Component.empty().append(comp).color(GREEN);
    }

    /**
     * Generate a component with the colour of AQUA.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component aqua(String format, Object... values) {
        return format(format, values).color(AQUA);
    }

    /**
     * Generate a component with the colour of AQUA using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component aqua(Object obj) {
        return aqua("{}", obj);
    }

    /**
     * Generate a component with the colour of AQUA
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component aqua(String str) {
        return Component.text(str).color(AQUA);
    }

    /**
     * Wrap a component with the colour of AQUA
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component aqua(Component comp) {
        return Component.empty().append(comp).color(AQUA);
    }

    /**
     * Generate a component with the colour of RED.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component red(String format, Object... values) {
        return format(format, values).color(RED);
    }

    /**
     * Generate a component with the colour of RED using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component red(Object obj) {
        return red("{}", obj);
    }

    /**
     * Generate a component with the colour of RED
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component red(String str) {
        return Component.text(str).color(RED);
    }

    /**
     * Wrap a component with the colour of RED
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component red(Component comp) {
        return Component.empty().append(comp).color(RED);
    }

    /**
     * Generate a component with the colour of LIGHT_PURPLE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component lightPurple(String format, Object... values) {
        return format(format, values).color(LIGHT_PURPLE);
    }

    /**
     * Generate a component with the colour of LIGHT_PURPLE using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component lightPurple(Object obj) {
        return lightPurple("{}", obj);
    }

    /**
     * Generate a component with the colour of LIGHT_PURPLE
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component lightPurple(String str) {
        return Component.text(str).color(LIGHT_PURPLE);
    }

    /**
     * Wrap a component with the colour of LIGHT_PURPLE
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component lightPurple(Component comp) {
        return Component.empty().append(comp).color(LIGHT_PURPLE);
    }

    /**
     * Generate a component with the colour of YELLOW.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component yellow(String format, Object... values) {
        return format(format, values).color(YELLOW);
    }

    /**
     * Generate a component with the colour of YELLOW using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component yellow(Object obj) {
        return yellow("{}", obj);
    }

    /**
     * Generate a component with the colour of YELLOW
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component yellow(String str) {
        return Component.text(str).color(YELLOW);
    }

    /**
     * Wrap a component with the colour of YELLOW
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component yellow(Component comp) {
        return Component.empty().append(comp).color(YELLOW);
    }

    /**
     * Generate a component with the colour of WHITE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component white(String format, Object... values) {
        return format(format, values).color(WHITE);
    }

    /**
     * Generate a component with the colour of WHITE using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component white(Object obj) {
        return white("{}", obj);
    }

    /**
     * Generate a component with the colour of WHITE
     *
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component white(String str) {
        return Component.text(str).color(WHITE);
    }

    /**
     * Wrap a component with the colour of WHITE
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component white(Component comp) {
        return Component.empty().append(comp).color(WHITE);
    }

    /**
     * Generate a component with the specified
     *
     * @param col    The colour to use
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component color(TextColor col, String format, Object... values) {
        return format(format, values).color(col);
    }

    /**
     * Generate a component with the colour specified using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param col The colour to use
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component color(TextColor col, Object obj) {
        return color(col, "{}", obj);
    }

    /**
     * Generate a component with the colour provided
     *
     * @param col The colour to use
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component color(TextColor col, String str) {
        return Component.text(str).color(col);
    }

    /**
     * Wrap a component with the colour provided
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component color(TextColor col, Component comp) {
        return Component.empty().append(comp).color(col);
    }

    /**
     * Generate a component with the specified
     *
     * @param col    The colour to use (Parsed with {@link FLUtils#parseColor(String)})
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    @Contract(pure = true)
    public static Component color(String col, String format, Object... values) {
        return format(format, values).color(FLUtils.parseColor(col));
    }

    /**
     * Generate a component with the colour specified using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param col The colour to use (Parsed with {@link FLUtils#parseColor(String)})
     * @param obj The object to format
     */
    @Contract(pure = true)
    public static Component color(String col, Object obj) {
        return color(col, "{}", obj);
    }

    /**
     * Generate a component with the colour provided
     *
     * @param col The colour to use (Parsed with {@link FLUtils#parseColor(String)})
     * @param str The string to use
     */
    @Contract(pure = true)
    public static Component color(String col, String str) {
        return Component.text(str).color(FLUtils.parseColor(col));
    }

    /**
     * Wrap a component with the colour provided
     *
     * @param comp The Component to wrap
     */
    @Contract(pure = true)
    public static Component color(String col, Component comp) {
        return Component.empty().append(comp).color(FLUtils.parseColor(col));
    }
}
