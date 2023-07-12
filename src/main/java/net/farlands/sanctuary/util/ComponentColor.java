package net.farlands.sanctuary.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import static net.farlands.sanctuary.util.ComponentUtils.format;

public class ComponentColor {


    /**
     * Generate a component with the colour of BLACK.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component black(String format, Object... values) {
        return format(format, values).color(NamedTextColor.BLACK);
    }

    /**
     * Generate a component with the colour of BLACK using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component black(Object obj) {
        return black("{}", obj);
    }

    /**
     * Generate a component with the colour of BLACK
     *
     * @param str The string to use
     */
    public static Component black(String str) {
        return Component.text(str).color(NamedTextColor.BLACK);
    }


    /**
     * Generate a component with the colour of DARK_BLUE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component darkBlue(String format, Object... values) {
        return format(format, values).color(NamedTextColor.DARK_BLUE);
    }

    /**
     * Generate a component with the colour of DARK_BLUE using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    public static Component darkBlue(Object obj) {
        return darkBlue("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_BLUE
     *
     * @param str The string to use
     */
    public static Component darkBlue(String str) {
        return Component.text(str).color(NamedTextColor.DARK_BLUE);
    }


    /**
     * Generate a component with the colour of DARK_GREEN.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component darkGreen(String format, Object... values) {
        return format(format, values).color(NamedTextColor.DARK_GREEN);
    }

    /**
     * Generate a component with the colour of DARK_GREEN using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    public static Component darkGreen(Object obj) {
        return darkGreen("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_GREEN
     *
     * @param str The string to use
     */
    public static Component darkGreen(String str) {
        return Component.text(str).color(NamedTextColor.DARK_GREEN);
    }


    /**
     * Generate a component with the colour of DARK_AQUA.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component darkAqua(String format, Object... values) {
        return format(format, values).color(NamedTextColor.DARK_AQUA);
    }

    /**
     * Generate a component with the colour of DARK_AQUA using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    public static Component darkAqua(Object obj) {
        return darkAqua("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_AQUA
     *
     * @param str The string to use
     */
    public static Component darkAqua(String str) {
        return Component.text(str).color(NamedTextColor.DARK_AQUA);
    }


    /**
     * Generate a component with the colour of DARK_RED.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component darkRed(String format, Object... values) {
        return format(format, values).color(NamedTextColor.DARK_RED);
    }

    /**
     * Generate a component with the colour of DARK_RED using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    public static Component darkRed(Object obj) {
        return darkRed("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_RED
     *
     * @param str The string to use
     */
    public static Component darkRed(String str) {
        return Component.text(str).color(NamedTextColor.DARK_RED);
    }


    /**
     * Generate a component with the colour of DARK_PURPLE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component darkPurple(String format, Object... values) {
        return format(format, values).color(NamedTextColor.DARK_PURPLE);
    }

    /**
     * Generate a component with the colour of DARK_PURPLE using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    public static Component darkPurple(Object obj) {
        return darkPurple("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_PURPLE
     *
     * @param str The string to use
     */
    public static Component darkPurple(String str) {
        return Component.text(str).color(NamedTextColor.DARK_PURPLE);
    }


    /**
     * Generate a component with the colour of GOLD.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component gold(String format, Object... values) {
        return format(format, values).color(NamedTextColor.GOLD);
    }

    /**
     * Generate a component with the colour of GOLD using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component gold(Object obj) {
        return gold("{}", obj);
    }

    /**
     * Generate a component with the colour of GOLD
     *
     * @param str The string to use
     */
    public static Component gold(String str) {
        return Component.text(str).color(NamedTextColor.GOLD);
    }


    /**
     * Generate a component with the colour of GRAY.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component gray(String format, Object... values) {
        return format(format, values).color(NamedTextColor.GRAY);
    }

    /**
     * Generate a component with the colour of GRAY using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component gray(Object obj) {
        return gray("{}", obj);
    }

    /**
     * Generate a component with the colour of GRAY
     *
     * @param str The string to use
     */
    public static Component gray(String str) {
        return Component.text(str).color(NamedTextColor.GRAY);
    }


    /**
     * Generate a component with the colour of DARK_GRAY.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component darkGray(String format, Object... values) {
        return format(format, values).color(NamedTextColor.DARK_GRAY);
    }

    /**
     * Generate a component with the colour of DARK_GRAY using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    public static Component darkGray(Object obj) {
        return darkGray("{}", obj);
    }

    /**
     * Generate a component with the colour of DARK_GRAY
     *
     * @param str The string to use
     */
    public static Component darkGray(String str) {
        return Component.text(str).color(NamedTextColor.DARK_GRAY);
    }


    /**
     * Generate a component with the colour of BLUE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component blue(String format, Object... values) {
        return format(format, values).color(NamedTextColor.BLUE);
    }

    /**
     * Generate a component with the colour of BLUE using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component blue(Object obj) {
        return blue("{}", obj);
    }

    /**
     * Generate a component with the colour of BLUE
     *
     * @param str The string to use
     */
    public static Component blue(String str) {
        return Component.text(str).color(NamedTextColor.BLUE);
    }


    /**
     * Generate a component with the colour of GREEN.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component green(String format, Object... values) {
        return format(format, values).color(NamedTextColor.GREEN);
    }

    /**
     * Generate a component with the colour of GREEN using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component green(Object obj) {
        return green("{}", obj);
    }

    /**
     * Generate a component with the colour of GREEN
     *
     * @param str The string to use
     */
    public static Component green(String str) {
        return Component.text(str).color(NamedTextColor.GREEN);
    }


    /**
     * Generate a component with the colour of AQUA.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component aqua(String format, Object... values) {
        return format(format, values).color(NamedTextColor.AQUA);
    }

    /**
     * Generate a component with the colour of AQUA using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component aqua(Object obj) {
        return aqua("{}", obj);
    }

    /**
     * Generate a component with the colour of AQUA
     *
     * @param str The string to use
     */
    public static Component aqua(String str) {
        return Component.text(str).color(NamedTextColor.AQUA);
    }


    /**
     * Generate a component with the colour of RED.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component red(String format, Object... values) {
        return format(format, values).color(NamedTextColor.RED);
    }

    /**
     * Generate a component with the colour of RED using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component red(Object obj) {
        return red("{}", obj);
    }

    /**
     * Generate a component with the colour of RED
     *
     * @param str The string to use
     */
    public static Component red(String str) {
        return Component.text(str).color(NamedTextColor.RED);
    }


    /**
     * Generate a component with the colour of LIGHT_PURPLE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component lightPurple(String format, Object... values) {
        return format(format, values).color(NamedTextColor.LIGHT_PURPLE);
    }

    /**
     * Generate a component with the colour of LIGHT_PURPLE using {@link ComponentUtils#format} with the format of
     * {@code {}}
     *
     * @param obj The object to format
     */
    public static Component lightPurple(Object obj) {
        return lightPurple("{}", obj);
    }

    /**
     * Generate a component with the colour of LIGHT_PURPLE
     *
     * @param str The string to use
     */
    public static Component lightPurple(String str) {
        return Component.text(str).color(NamedTextColor.LIGHT_PURPLE);
    }


    /**
     * Generate a component with the colour of YELLOW.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component yellow(String format, Object... values) {
        return format(format, values).color(NamedTextColor.YELLOW);
    }

    /**
     * Generate a component with the colour of YELLOW using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component yellow(Object obj) {
        return yellow("{}", obj);
    }

    /**
     * Generate a component with the colour of YELLOW
     *
     * @param str The string to use
     */
    public static Component yellow(String str) {
        return Component.text(str).color(NamedTextColor.YELLOW);
    }


    /**
     * Generate a component with the colour of WHITE.
     *
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component white(String format, Object... values) {
        return format(format, values).color(NamedTextColor.WHITE);
    }

    /**
     * Generate a component with the colour of WHITE using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param obj The object to format
     */
    public static Component white(Object obj) {
        return white("{}", obj);
    }

    /**
     * Generate a component with the colour of WHITE
     *
     * @param str The string to use
     */
    public static Component white(String str) {
        return Component.text(str).color(NamedTextColor.WHITE);
    }

    /**
     * Generate a component with the specified
     *
     * @param col    The colour to use
     * @param format The text to use for the format, uses {@link ComponentUtils#format}
     * @param values The values to use for the format
     */
    public static Component color(TextColor col, String format, Object... values) {
        return format(format, values).color(col);
    }

    /**
     * Generate a component with the colour specified using {@link ComponentUtils#format} with the format of {@code {}}
     *
     * @param col The colour to use
     * @param obj The object to format
     */
    public static Component color(TextColor col, Object obj) {
        return color(col, "{}", obj);
    }

    /**
     * Generate a component with the colour of WHITE
     *
     * @param col The colour to use
     * @param str The string to use
     */
    public static Component color(TextColor col, String str) {
        return Component.text(str).color(col);
    }
}
