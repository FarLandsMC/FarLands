package net.farlands.sanctuary.util;

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
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.util.Collection;

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
     * @param command     The command to show
     * @param fillCommand The command to fill with
     * @param color       The color of the base text
     * @return The full component
     */
    public static Component suggestCommand(String command, String fillCommand, TextColor color) {
        Component c = Component.text(command).color(color);
        return suggestCommand(fillCommand, c, ComponentColor.gray("Fill Command"));
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
}
