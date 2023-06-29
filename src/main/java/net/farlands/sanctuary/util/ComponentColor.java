package net.farlands.sanctuary.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ComponentColor {

    /**
     * Generate a component with the color of BLACK
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component black(String text, Object... values) {
        return format(text, values).color(NamedTextColor.BLACK);
    }

    /**
     * Generate a component with the color of BLACK using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component black(Object obj) {
        return format(obj).color(NamedTextColor.BLACK);
    }

    /**
     * Generate a component with the color of DARK_BLUE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkBlue(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_BLUE);
    }

    /**
     * Generate a component with the color of DARK_BLUE using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component darkBlue(Object obj) {
        return format(obj).color(NamedTextColor.DARK_BLUE);
    }

    /**
     * Generate a component with the color of DARK_GREEN
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkGreen(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_GREEN);
    }

    /**
     * Generate a component with the color of DARK_GREEN using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component darkGreen(Object obj) {
        return format(obj).color(NamedTextColor.DARK_GREEN);
    }

    /**
     * Generate a component with the color of DARK_AQUA
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkAqua(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_AQUA);
    }

    /**
     * Generate a component with the color of DARK_AQUA using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component darkAqua(Object obj) {
        return format(obj).color(NamedTextColor.DARK_AQUA);
    }

    /**
     * Generate a component with the color of DARK_RED
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkRed(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_RED);
    }

    /**
     * Generate a component with the color of DARK_RED using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component darkRed(Object obj) {
        return format(obj).color(NamedTextColor.DARK_RED);
    }

    /**
     * Generate a component with the color of DARK_PURPLE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkPurple(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_PURPLE);
    }

    /**
     * Generate a component with the color of DARK_PURPLE using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component darkPurple(Object obj) {
        return format(obj).color(NamedTextColor.DARK_PURPLE);
    }

    /**
     * Generate a component with the color of GOLD
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component gold(String text, Object... values) {
        return format(text, values).color(NamedTextColor.GOLD);
    }

    /**
     * Generate a component with the color of GOLD using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component gold(Object obj) {
        return format(obj).color(NamedTextColor.GOLD);
    }

    /**
     * Generate a component with the color of GRAY
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component gray(String text, Object... values) {
        return format(text, values).color(NamedTextColor.GRAY);
    }

    /**
     * Generate a component with the color of GRAY using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component gray(Object obj) {
        return format(obj).color(NamedTextColor.GRAY);
    }

    /**
     * Generate a component with the color of DARK_GRAY
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkGray(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_GRAY);
    }

    /**
     * Generate a component with the color of DARK_GRAY using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component darkGray(Object obj) {
        return format(obj).color(NamedTextColor.DARK_GRAY);
    }

    /**
     * Generate a component with the color of BLUE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component blue(String text, Object... values) {
        return format(text, values).color(NamedTextColor.BLUE);
    }

    /**
     * Generate a component with the color of BLUE using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component blue(Object obj) {
        return format(obj).color(NamedTextColor.BLUE);
    }

    /**
     * Generate a component with the color of GREEN
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component green(String text, Object... values) {
        return format(text, values).color(NamedTextColor.GREEN);
    }

    /**
     * Generate a component with the color of GREEN using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component green(Object obj) {
        return format(obj).color(NamedTextColor.GREEN);
    }

    /**
     * Generate a component with the color of AQUA
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component aqua(String text, Object... values) {
        return format(text, values).color(NamedTextColor.AQUA);
    }

    /**
     * Generate a component with the color of AQUA using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component aqua(Object obj) {
        return format(obj).color(NamedTextColor.AQUA);
    }

    /**
     * Generate a component with the color of RED
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component red(String text, Object... values) {
        return format(text, values).color(NamedTextColor.RED);
    }

    /**
     * Generate a component with the color of RED using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component red(Object obj) {
        return format(obj).color(NamedTextColor.RED);
    }

    /**
     * Generate a component with the color of LIGHT_PURPLE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component lightPurple(String text, Object... values) {
        return format(text, values).color(NamedTextColor.LIGHT_PURPLE);
    }

    /**
     * Generate a component with the color of LIGHT_PURPLE using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component lightPurple(Object obj) {
        return format(obj).color(NamedTextColor.LIGHT_PURPLE);
    }

    /**
     * Generate a component with the color of YELLOW
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component yellow(String text, Object... values) {
        return format(text, values).color(NamedTextColor.YELLOW);
    }

    /**
     * Generate a component with the color of YELLOW using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component yellow(Object obj) {
        return format(obj).color(NamedTextColor.YELLOW);
    }

    /**
     * Generate a component with the color of WHITE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in
     *               fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component white(String text, Object... values) {
        return format(text, values).color(NamedTextColor.WHITE);
    }

    /**
     * Generate a component with the color of WHITE using {@link String#valueOf}
     *
     * @param obj The object to convert into a string with {@link String#valueOf}
     */
    public static Component white(Object obj) {
        return format(obj).color(NamedTextColor.WHITE);
    }

    /**
     * Generate a component from text using replacements
     * <p>
     * Escapes <code>%</code> if there's no values
     */
    private static Component format(String text, Object... values) {
        return Component.text(String.format(values.length > 0 ? text : text.replace("%", "%%"), values));
    }

    /**
     * Generate a component from an object using {@link String#valueOf}
     */
    private static Component format(Object obj) {
        return Component.text(String.valueOf(obj));
    }

}
