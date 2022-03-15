package net.farlands.sanctuary.chat;

import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.Buildable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A wrapper for {@link MiniMessage} to add a few more methods for more customization.
 *
 * @author Majekdor
 */
public interface MiniMessageWrapper extends Buildable<MiniMessageWrapper, MiniMessageWrapper.Builder> {

    /**
     * <p>Gets a simple instance.</p>
     * <p>This will parse everything like {@link MiniMessage} will except for advanced transformations.</p>
     * <p>Builder options with this instance:</p>
     * <ul>
     *   <li>Gradients: True</li>
     *   <li>Hex Colors: True</li>
     *   <li>Standard Colors: True</li>
     *   <li>Legacy Colors: False</li>
     *   <li>Advanced Transformations: False</li>
     *   <li>Placeholder Resolver: Empty</li>
     *   <li>Block Text Decorations: None</li>
     *   <li>Blocked Colors: None</li>
     *   <li>Luminance Threshold: 0</li>
     * </ul>
     *
     * @return a simple instance
     */
    static @NotNull MiniMessageWrapper standard() {
        return MiniMessageWrapperImpl.STANDARD;
    }

    /**
     * <p>Gets a simple instance with legacy code support.</p>
     * <p>This will parse everything like {@link MiniMessage} will with the addition of
     * legacy code support and the subtraction of advanced transformation support.</p>
     * <p>Builder options with this instance:</p>
     * <ul>
     *   <li>Gradients: True</li>
     *   <li>Hex Colors: True</li>
     *   <li>Standard Colors: True</li>
     *   <li>Legacy Colors: True</li>
     *   <li>Advanced Transformations: False</li>
     *   <li>Placeholder Resolver: Empty</li>
     *   <li>Block Text Decorations: None</li>
     *   <li>Blocked Colors: None</li>
     *   <li>Luminance Threshold: 0</li>
     * </ul>
     *
     * @return a simple instance
     */
    static @NotNull MiniMessageWrapper legacy() {
        return MiniMessageWrapperImpl.LEGACY;
    }

    /**
     * <p>Gets the standard FarLands instance.</p>
     * <p>Builder options with this instance:</p>
     * <ul>
     *   <li>Gradients: If rank is adept or above</li>
     *   <li>Hex Colors: If rank is adept or above</li>
     *   <li>Standard Colors: If rank is adept or above</li>
     *   <li>Legacy Colors: If rank is adept or above</li>
     *   <li>Advanced Transformations: If rank is staff</li>
     *   <li>Placeholder Resolver: Empty</li>
     *   <li>Block Text Decorations: Obfuscated</li>
     *   <li>Blocked Colors: Black</li>
     *   <li>Luminance Threshold: 16</li>
     * </ul>
     *
     * @param flp the {@link OfflineFLPlayer} this parsing is related to
     * @return an instance with farlands settings
     */
    static @NotNull MiniMessageWrapper farlands(final @NotNull OfflineFLPlayer flp) {
        return new MiniMessageWrapperImpl(
            flp.rank.specialCompareTo(Rank.ADEPT) >= 0,
            flp.rank.specialCompareTo(Rank.ADEPT) >= 0,
            flp.rank.specialCompareTo(Rank.ADEPT) >= 0,
            flp.rank.specialCompareTo(Rank.ADEPT) >= 0,
            flp.rank.isStaff(),
            true,
            TagResolver.empty(),
            Set.of(TextDecoration.OBFUSCATED),
            Set.of(NamedTextColor.BLACK),
            16
        );
    }

    /**
     * Parse a string into a {@link Component} using {@link MiniMessage}.
     *
     * @param mmString the string to parse
     * @return component
     */
    @NotNull Component mmParse(@NotNull String mmString);

    /**
     * Get the modified string.
     *
     * @param mmString string to modify
     * @return modified string
     */
    @NotNull String mmString(@NotNull String mmString);

    /**
     * <p>Creates a new {@link Builder}.</p>
     * <p>Default builder options:</p>
     * <ul>
     *   <li>Gradients: True</li>
     *   <li>Hex Colors: True</li>
     *   <li>Standard Colors: True</li>
     *   <li>Legacy Colors: False</li>
     *   <li>Advanced Transformations: False</li>
     * </ul>
     *
     * @return a builder
     */
    static @NotNull Builder builder() {
        return new MiniMessageWrapperImpl.BuilderImpl();
    }

    /**
     * Create a {@link Builder} to modify options.
     *
     * @return a builder
     */
    @Override
    @NotNull Builder toBuilder();

    /**
     * A builder for {@link MiniMessageWrapper}.
     */
    interface Builder extends Buildable.Builder<MiniMessageWrapper> {

        /**
         * Whether gradients on the final string should be parsed.
         *
         * @param parse whether to parse
         * @return this builder
         */
        @NotNull Builder gradients(final boolean parse);

        /**
         * Whether hex colors on the final string should be parsed.
         *
         * @param parse whether to parse
         * @return this builder
         */
        @NotNull Builder hexColors(final boolean parse);

        /**
         * Whether all standard color codes on the final string should be parsed.
         *
         * @param parse whether to parse
         * @return this builder
         */
        @NotNull Builder standardColors(final boolean parse);

        /**
         * Whether legacy color codes on the final string should be parsed.
         *
         * @param parse whether to parse
         * @return this builder
         */
        @NotNull Builder legacyColors(final boolean parse);

        /**
         * Whether to parse advanced {@link Tag}s on the final string to be parsed.
         * This includes click events, hover events, fonts, etc.
         *
         * @param parse whether to parse
         * @return this builder
         */
        @NotNull Builder advancedTransformations(final boolean parse);

        /**
         * The {@link TextDecoration}s that should not be parsed.
         *
         * @param decorations the decorations
         * @return this builder
         */
        @NotNull Builder removeTextDecorations(final @NotNull TextDecoration... decorations);

        /**
         * Set the {@link TagResolver} for placeholders for the {@link MiniMessage} instance.
         *
         * @param placeholderResolver the placeholder resolver
         * @return this builder
         */
        @NotNull Builder placeholderResolver(final @NotNull TagResolver placeholderResolver);

        /**
         * The {@link NamedTextColor}s that should not be parsed.
         *
         * @param blockCloseHex whether to block hex codes that are close to blocked colors
         * @param colors        the colors
         * @return this builder
         */
        @NotNull Builder removeColors(final boolean blockCloseHex, final @NotNull NamedTextColor... colors);

        /**
         * <p>Prevent hex colors that have a luminance below a certain threshold.
         * Luminance is measure 0 - 255. There's a method in {@link MiniMessageWrapperImpl}
         * that is used to calculate it. This is typically used to prevent very dark colors.</p>
         * <p>Note: If you want to block the normal black color, remember to add
         * it to {@link #removeColors(boolean, NamedTextColor...)}</p>
         *
         * @param threshold all colors with luminance below this will not be parsed
         * @return this builder
         */
        @NotNull Builder preventLuminanceBelow(final int threshold);

        /**
         * Build the {@link MiniMessageWrapper} ready to parse.
         *
         * @return the wrapper
         */
        @Override
        @NotNull MiniMessageWrapper build();
    }
}
