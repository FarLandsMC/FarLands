package net.farlands.sanctuary.chat;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.LinkedList;
import java.util.List;

/**
 * Easily manage multiple pages of components.
 */
public class Pagination {

    private final List<Component> lines;
    private final Component header;
    private final String movePageCommand;

    private Component footer = Component.empty();
    private Component nextPageButton = Component.text("Next Page »");
    private Component prevPageButton = Component.text("« Prev Page");
    private TextColor validButtonColor = NamedTextColor.GREEN;
    private TextColor invalidButtonColor = NamedTextColor.RED;
    private TextColor pageCounterColor = NamedTextColor.GOLD;
    private TextColor hoverTextColor = NamedTextColor.GRAY;
    private int linesPerPage = 8;
    private int maxChatWidth = 55;

    /**
     * Create a new pagination.
     *
     * @param header          the header for the top of the page
     * @param movePageCommand the command for traversing pages of the pagination, you don't need to
     *                        include the space, the space and page number will be appended
     */
    public Pagination(final @NotNull Component header, final @NotNull String movePageCommand) {
        this.lines = new LinkedList<>();
        this.header = header;
        this.movePageCommand = movePageCommand.trim() + " ";
    }

    /**
     * Add a line to this pagination. If the line is longer than the maximum width it
     * will be cut into multiple lines.
     *
     * @param line the line to add.
     * @return this pagination
     */
    public @NotNull Pagination addLine(final @NotNull Component line) {
        if (this.plainText(line).length() > this.maxChatWidth) {
            this.lines.addAll(this.cut(line));
        } else {
            this.lines.add(line);
        }
        return this;
    }

    /**
     * Add lines to this pagination. If a line is longer than the maximum width it
     * will be cut into multiple lines.
     *
     * @param lines the lines to add.
     * @return this pagination
     */
    public @NotNull Pagination addLines(final @NotNull List<Component> lines) {
        lines.forEach(line -> {
            if (this.plainText(line).length() > this.maxChatWidth) {
                this.lines.addAll(this.cut(line));
            } else {
                this.lines.add(line);
            }
        });
        return this;
    }

    /**
     * Add lines to this pagination. If a line is longer than the maximum width it
     * will be cut into multiple lines.
     *
     * @param lines the lines to add.
     * @return this pagination
     */
    public @NotNull Pagination addLines(final @NotNull Component... lines) {
        for (Component line : lines) {
            if (this.plainText(line).length() > this.maxChatWidth) {
                this.lines.addAll(this.cut(line));
            } else {
                this.lines.add(line);
            }
        }
        return this;
    }

    /**
     * Get the number of pages for this pagination.
     *
     * @return number of pages
     */
    public int numPages() {
        return (int) Math.ceil((double) this.lines.size() / this.linesPerPage);
    }

    /**
     * Render a page. This will return a list of lines for the page, including the
     * header at the top and footer at the bottom. You can use {@link #sendPage(int, Audience)}
     * to easily send a specific page to an audience.
     *
     * @param page the page to render
     * @return list of lines for the desired page
     */
    public @NotNull List<Component> render(final @Range(from = 1, to = Integer.MAX_VALUE) int page) {
        if (page > this.numPages()) {
            throw new InvalidPageException();
        }

        final List<Component> lines = new LinkedList<>();
        final boolean hasPrevPage = page > 1;
        final boolean hasNextPage = page < this.numPages();

        Component header = Component.empty();

        if (hasPrevPage) {
            header = header.append(
                this.prevPageButton.color(this.validButtonColor)
                    .hoverEvent(
                        HoverEvent.showText(Component.text("Click to go to the previous page.", this.hoverTextColor))
                    )
                    .clickEvent(ClickEvent.runCommand(this.movePageCommand + (page - 1)))
            );
        } else {
            header = header.append(
                this.prevPageButton.color(this.invalidButtonColor)
                    .hoverEvent(HoverEvent.showText(Component.text("No previous page.", this.hoverTextColor)))
            );
        }

        header = header.append(Component.space()).append(this.header()).append(Component.text(" - Page " +
            page + "/" + this.numPages()).color(this.pageCounterColor)).append(Component.space());

        if (hasNextPage) {
            header = header.append(
                this.nextPageButton.color(this.validButtonColor)
                    .hoverEvent(
                        HoverEvent.showText(Component.text("Click to go to the next page.", this.hoverTextColor))
                    )
                    .clickEvent(ClickEvent.runCommand(this.movePageCommand + (page + 1)))
            );
        } else {
            header = header.append(
                this.nextPageButton.color(this.invalidButtonColor)
                    .hoverEvent(HoverEvent.showText(Component.text("No next page.", this.hoverTextColor)))
            );
        }

        lines.add(header);

        int currentLine = ((page - 1) * this.linesPerPage);
        for (int i = 0; i < this.linesPerPage; i++) {
            if (currentLine >= this.lines.size()) {
                break;
            }
            lines.add(this.lines.get(currentLine));
            currentLine++;
        }

        if (this.plainText(this.footer).length() > 0) {
            lines.add(this.footer);
        }

        return lines;
    }

    /**
     * Send a rendered page to an audience.
     *
     * @param page     the page to render
     * @param audience the audience to send the page to
     */
    public void sendPage(final @Range(from = 1, to = Integer.MAX_VALUE) int page, final @NotNull Audience audience) {
        for (Component line : this.render(page)) {
            audience.sendMessage(line);
        }
    }

    /**
     * Get a list of all lines for the pagination.
     *
     * @return all lines
     */
    public @NotNull List<Component> lines() {
        return this.lines;
    }

    /**
     * Get the header for this pagination.
     *
     * @return the header
     */
    public @NotNull Component header() {
        return this.header;
    }

    /**
     * Get the footer for this pagination.
     *
     * @return the footer
     */
    public @NotNull Component footer() {
        return this.footer;
    }

    /**
     * Set the footer.
     *
     * @param footer the footer
     * @return this pagination
     */
    public @NotNull Pagination footer(final @NotNull Component footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Get the component that will be used as a button for traveling to the next page.
     *
     * @return the next page button
     */
    public @NotNull Component nextPageButton() {
        return this.nextPageButton;
    }

    /**
     * Set the component that will be used as a button for traveling to the next page.
     *
     * @param nextPageButton the next page button
     * @return this pagination
     */
    public @NotNull Pagination nextPageButton(final @NotNull Component nextPageButton) {
        this.nextPageButton = nextPageButton;
        return this;
    }

    /**
     * Get the component that will be used as a button for traveling to the previous page.
     *
     * @return the previous page button
     */
    public @NotNull Component prevPageButton() {
        return this.prevPageButton;
    }

    /**
     * Set the component that will be used as a button for traveling to the previous page.
     *
     * @param prevPageButton the previous page button
     * @return this pagination
     */
    public @NotNull Pagination prevPageButton(final @NotNull Component prevPageButton) {
        this.prevPageButton = prevPageButton;
        return this;
    }

    /**
     * Get the text color that will be used for valid buttons.
     *
     * @return the valid button color
     */
    public @NotNull TextColor validButtonColor() {
        return validButtonColor;
    }

    /**
     * Set the text color that will be used for valid buttons.
     *
     * @param validButtonColor the valid button color
     * @return this pagination
     */
    public @NotNull Pagination validButtonColor(final @NotNull TextColor validButtonColor) {
        this.validButtonColor = validButtonColor;
        return this;
    }

    /**
     * Get the text color that will be used for invalid buttons.
     *
     * @return the invalid button color
     */
    public @NotNull TextColor invalidButtonColor() {
        return this.invalidButtonColor;
    }

    /**
     * Set the text color that will be used for invalid buttons.
     *
     * @param invalidButtonColor the invalid button color
     * @return this pagination
     */
    public @NotNull Pagination invalidButtonColor(final @NotNull TextColor invalidButtonColor) {
        this.invalidButtonColor = invalidButtonColor;
        return this;
    }

    /**
     * Get the text color that will be used for the page counter.
     *
     * @return the page counter color
     */
    public @NotNull TextColor pageCounterColor() {
        return this.pageCounterColor;
    }

    /**
     * Set the text color that will be used for the page counter.
     *
     * @param pageCounterColor the page counter color
     * @return this pagination
     */
    public @NotNull Pagination pageCounterColor(final @NotNull TextColor pageCounterColor) {
        this.pageCounterColor = pageCounterColor;
        return this;
    }

    /**
     * Get the text color that will be used for hover text.
     *
     * @return the hover text color
     */
    public @NotNull TextColor hoverTextColor() {
        return this.hoverTextColor;
    }

    /**
     * Set the text color that will be used for hover text.
     *
     * @param hoverTextColor the hover text color
     * @return this pagination
     */
    public @NotNull Pagination hoverTextColor(final @NotNull TextColor hoverTextColor) {
        this.hoverTextColor = hoverTextColor;
        return this;
    }

    /**
     * Get the number of lines per page, not counting the header or footer.
     *
     * @return the lines per page
     */
    public int linesPerPage() {
        return this.linesPerPage;
    }

    /**
     * Set the number of lines per page, not counting the header or footer.
     *
     * @param linesPerPage the lines per page
     * @return this pagination
     */
    public @NotNull Pagination linesPerPage(final @Range(from = 0, to = Integer.MAX_VALUE) int linesPerPage) {
        this.linesPerPage = linesPerPage;
        return this;
    }

    /**
     * Get the maximum amount of characters allowed per line.
     * Lines exceeding this value will be cut into separate lines.
     *
     * @return the max chat width
     */
    public int maxChatWidth() {
        return this.maxChatWidth;
    }

    /**
     * Set the maximum amount of characters allowed per line.
     * Lines exceeding this value will be cut into separate lines.
     *
     * @param maxChatWidth the max chat width
     * @return this pagination
     */
    public @NotNull Pagination maxChatWidth(final @Range(from = 0, to = Integer.MAX_VALUE) int maxChatWidth) {
        this.maxChatWidth = maxChatWidth;
        return this;
    }

    /**
     * Serialize a component to plain text via the {@link PlainTextComponentSerializer}.
     *
     * @param component the component
     * @return component content
     */
    private @NotNull String plainText(final @NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Cut a component into multiple components via the {@link TextComponentCutter}.
     *
     * @param component the component to cut
     * @return multiple components.
     */
    private @NotNull List<Component> cut(@NotNull Component component) {
        return new TextComponentCutter(this.maxChatWidth - 5, this.maxChatWidth + 5).cutComponent(component);
    }

    /**
     * Thrown if an invalid page is provided in {@link #render(int)}.
     */
    public static class InvalidPageException extends IllegalArgumentException {

    }
}
