package net.farlands.sanctuary.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ComponentColor {

    /**
     * Generate a component with the color of BLACK
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component black(String text, Object... values) {
        return format(text, values).color(NamedTextColor.BLACK);
    }

    /**
     * Generate a component with the color of DARK_BLUE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkBlue(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_BLUE);
    }

    /**
     * Generate a component with the color of DARK_GREEN
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkGreen(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_GREEN);
    }

    /**
     * Generate a component with the color of DARK_AQUA
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkAqua(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_AQUA);
    }

    /**
     * Generate a component with the color of DARK_RED
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkRed(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_RED);
    }

    /**
     * Generate a component with the color of DARK_PURPLE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkPurple(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_PURPLE);
    }

    /**
     * Generate a component with the color of GOLD
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component gold(String text, Object... values) {
        return format(text, values).color(NamedTextColor.GOLD);
    }

    /**
     * Generate a component with the color of GRAY
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component gray(String text, Object... values) {
        return format(text, values).color(NamedTextColor.GRAY);
    }

    /**
     * Generate a component with the color of DARK_GRAY
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component darkGray(String text, Object... values) {
        return format(text, values).color(NamedTextColor.DARK_GRAY);
    }

    /**
     * Generate a component with the color of BLUE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component blue(String text, Object... values) {
        return format(text, values).color(NamedTextColor.BLUE);
    }

    /**
     * Generate a component with the color of GREEN
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component green(String text, Object... values) {
        return format(text, values).color(NamedTextColor.GREEN);
    }

    /**
     * Generate a component with the color of AQUA
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component aqua(String text, Object... values) {
        return format(text, values).color(NamedTextColor.AQUA);
    }

    /**
     * Generate a component with the color of RED
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component red(String text, Object... values) {
        return format(text, values).color(NamedTextColor.RED);
    }

    /**
     * Generate a component with the color of LIGHT_PURPLE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component lightPurple(String text, Object... values) {
        return format(text, values).color(NamedTextColor.LIGHT_PURPLE);
    }

    /**
     * Generate a component with the color of YELLOW
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component yellow(String text, Object... values) {
        return format(text, values).color(NamedTextColor.YELLOW);
    }

    /**
     * Generate a component with the color of WHITE
     *
     * @param text   The text to use for the replacement, uses {@link String#format(String, Object...)} to fill in fields
     * @param values The replacements, uses {@link String#format(String, Object...)} to fill in fields
     */
    public static Component white(String text, Object... values) {
        return format(text, values).color(NamedTextColor.WHITE);
    }

    /**
     * Generate a component from text using replacements
     * <p>
     * Escapes <code>%</code> if there's no values
     */
    private static Component format(String text, Object... values) {
        return Component.text(String.format(values.length > 0 ? text : text.replace("%", "%%"), values));
    }

}
