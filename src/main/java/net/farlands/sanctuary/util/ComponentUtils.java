package net.farlands.sanctuary.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.awt.*;

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
        return command(command, c, ComponentColor.gray("Click to Run."));
    }

    /**
     * Create a component with the styling and actions for a command
     *
     * @param command The command
     * @param base    The base component
     */
    public static Component command(String command, Component base) {
        return command(command, base, ComponentColor.gray("Click to Run."));
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
     * @param command     The command to show
     * @param fillCommand The command to fill with
     * @param color       The color of the base text
     * @return The full component
     */
    public static Component suggestCommand(String command, String fillCommand, TextColor color) {
        Component c = Component.text(command).color(color);
        return suggestCommand(fillCommand, c, ComponentColor.gray("Click to Fill."));
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
            .hoverEvent(HoverEvent.showText(ComponentColor.gray("Click to Open.")));
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
        return item(ComponentColor.aqua(FLUtils.itemName(item)), item);
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
     * @param color The color to convert from
     * @return The converted color
     */
    public static Color getColor(TextColor color) {
        return new Color(color.value());
    }
}
