package net.farlands.sanctuary.chat;

import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry;
import net.kyori.adventure.text.minimessage.transformation.TransformationType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

/**
 * Not public api.
 *
 * @author Majekdor
 */
@ApiStatus.Internal
final class MiniMessageWrapperImpl implements MiniMessageWrapper {

  @ApiStatus.Internal
  static final MiniMessageWrapper STANDARD = new MiniMessageWrapperImpl(true, true, true,
      false, false, true, PlaceholderResolver.empty(), new HashSet<>(), new HashSet<>(), 0);

  @ApiStatus.Internal
  static final MiniMessageWrapper LEGACY = new MiniMessageWrapperImpl(true, true, true,
      true, false, true, PlaceholderResolver.empty(), new HashSet<>(), new HashSet<>(), 0);

  @SuppressWarnings("all")
  private final TransformationRegistry allTransformations = TransformationRegistry.builder().clear().add(
      TransformationType.CLICK_EVENT, TransformationType.COLOR, TransformationType.DECORATION,
      TransformationType.FONT, TransformationType.GRADIENT, TransformationType.HOVER_EVENT,
      TransformationType.INSERTION, TransformationType.KEYBIND, TransformationType.RAINBOW,
      TransformationType.TRANSLATABLE
  ).build();

  @SuppressWarnings("all")
  private final TransformationRegistry colorTransformations = TransformationRegistry.builder().clear().add(
      TransformationType.COLOR, TransformationType.DECORATION,
      TransformationType.GRADIENT, TransformationType.RAINBOW
  ).build();

  private final boolean gradients, hexColors, standardColors, legacyColors, advancedTransformations, blockCloseHex;
  private final PlaceholderResolver placeholderResolver;
  private final Set<TextDecoration> removedTextDecorations;
  private final Set<NamedTextColor> removedColors;
  private final int luminanceThreshold;

  MiniMessageWrapperImpl(final boolean gradients, final boolean hexColors, final boolean standardColors,
                         final boolean legacyColors, final boolean advancedTransformations,
                         final boolean blockCloseHex, final PlaceholderResolver placeholderResolver,
                         final Set<TextDecoration> removedTextDecorations,
                         final Set<NamedTextColor> removedColors, final int luminanceThreshold) {
    this.gradients = gradients;
    this.hexColors = hexColors;
    this.standardColors = standardColors;
    this.legacyColors = legacyColors;
    this.advancedTransformations = advancedTransformations;
    this.blockCloseHex = blockCloseHex;
    this.placeholderResolver = placeholderResolver;
    this.removedTextDecorations = removedTextDecorations;
    this.removedColors = removedColors;
    this.luminanceThreshold = luminanceThreshold;
  }

  @Override
  public @NotNull Component mmParse(@NotNull String mmString) {
    Map<TextDecoration, TextDecoration.State> decorationStateMap = new HashMap<>();
    for (TextDecoration decoration : removedTextDecorations) {
      decorationStateMap.put(decoration, TextDecoration.State.FALSE);
    }
    return MiniMessage.builder().placeholderResolver(this.placeholderResolver).transformations(
        this.advancedTransformations ? this.allTransformations : this.colorTransformations
    ).build().parse(this.mmString(mmString)).decorations(decorationStateMap);
  }

  @Override
  public @NotNull String mmString(@NotNull String mmString) {
    for (NamedTextColor color : this.removedColors) {
      mmString = mmString.replace("<" + color.toString().toLowerCase(Locale.ROOT) + ">", "");
      mmString = mmString.replace("</" + color.toString().toLowerCase(Locale.ROOT) + ">", "");
      mmString = mmString.replace("&" + legacyCodeFromNamed(color), "");
    }

    if (this.legacyColors) {
      final Pattern legacyCharPattern = Pattern.compile("(?<!\\\\)(&([a-f0-9l-or]))");
      mmString = legacyCharPattern.matcher(mmString).replaceAll((result) -> CHAR_COLORS.get(result.group(2).charAt(0)));

      final Pattern escapedLegacyCharPattern = Pattern.compile("(\\\\&([a-f0-9l-or]))");
      mmString = escapedLegacyCharPattern.matcher(mmString).replaceAll((result) -> "&" + result.group(2));

      if (this.hexColors) {
        // parse the nicer pattern: '&#rrggbb' to spigot's: '&x&r&r&g&g&b&b'
        final Pattern sixCharHex = Pattern.compile("&#([0-9a-fA-F]{6})");
        Matcher matcher = sixCharHex.matcher(mmString);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
          final StringBuilder replacement = new StringBuilder(14).append("&x");
          for (final char character : matcher.group(1).toCharArray()) {
            replacement.append('&').append(character);
          }
          matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        mmString = sb.toString();

        // convert three char nicer hex '&#rgb' to spigot's: '&x&r&r&g&g&b&b'
        final Pattern threeCharHex = Pattern.compile("&#([0-9a-fA-F]{3})");
        matcher = threeCharHex.matcher(mmString);
        sb = new StringBuilder();
        while (matcher.find()) {
          final StringBuilder replacement = new StringBuilder(14).append("&x");
          for (final char character : matcher.group(1).toCharArray()) {
            replacement.append('&').append(character).append("&").append(character);
          }
          matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        mmString = sb.toString();

        // parse spigot's hex pattern '&x&r&r&g&g&b&b' to mini message's '<#rrggbb>'
        final Pattern spigotHexPattern = Pattern.compile("&x(&[0-9a-fA-F]){6}");
        matcher = spigotHexPattern.matcher(mmString);
        sb = new StringBuilder();
        while (matcher.find()) {
          final StringBuilder replacement = new StringBuilder(9).append("<#");
          for (final char character : matcher.group().toCharArray()) {
            if (character != '&' && character != 'x') {
              replacement.append(character);
            }
          }
          replacement.append(">");
          matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        mmString = sb.toString();
      } else {
        mmString = mmString.replaceAll("&#([0-9a-fA-F]{6})", "");
        mmString = mmString.replaceAll("&x(&[0-9a-fA-F]){6}", "");
      }
    } else {
      mmString = mmString.replaceAll("(&[0-9a-fA-Fk-oK-OxXrR])+", "");
    }

    if (!this.gradients) {
      mmString = mmString.replaceAll("<gradient([:#0-9a-fA-F]{8})+>", "");
      mmString = mmString.replaceAll("</gradient>", "");
    }

    final Pattern hexColorPattern = Pattern.compile("(<(/|)(c|color|colour|)(:|)(#[0-9a-fA-F]{6}|)>)");

    if (!this.hexColors) {
      mmString = hexColorPattern.matcher(mmString).replaceAll("");
    }

    final Pattern hexColorNoClosing = Pattern.compile("(<(c|color|colour|)(:|)(#[0-9a-fA-F]{6})>)");
    final Matcher matcher = hexColorNoClosing.matcher(mmString);
    while(matcher.find()) {
      try {
        String hexMatch = matcher.group(4);
        if (
            (blockCloseHex &&
                this.removedColors.contains(NamedTextColor.nearestTo(TextColor.color(Integer.parseInt(hexMatch.substring(1), 16))))) ||
                getLuma(hexMatch) < this.luminanceThreshold
        ) {
          mmString = mmString.replaceAll(matcher.group(1), "");
        }
      } catch (IndexOutOfBoundsException ignored) {
        // if there's no hex group we don't need to do anything
      }
    }

    // can't use regex, it would mess with placeholders
    if (!this.standardColors) {
      List<String> mmColorTags = new ArrayList<>(Arrays.asList("<black>", "<dark_blue>", "<dark_green>",
          "<dark_aqua>", "<dark_red>", "<dark_purple>", "<gold>", "<gray>", "<dark_gray>", "<blue>", "<green>",
          "<aqua>", "<red>", "<light_purple>", "<yellow>", "<white>", "<underlined>", "<strikethrough>", "<st>",
          "<obfuscated>", "<obf>", "<italic>", "<em>", "<i>", "<bold>", "<b>", "<reset>", "<r>", "<pre>",
          "</black>", "</dark_blue>", "</dark_green>", "</dark_aqua>", "</dark_red>", "</dark_purple>", "</gold>",
          "</gray>", "</dark_gray>", "</blue>", "</green>", "</aqua>", "</red>", "</light_purple>", "</yellow>",
          "</white>", "</underlined>", "</strikethrough>", "</st>", "</obfuscated>", "</obf>", "</italic>",
          "</em>", "</i>", "</bold>", "</b>", "</reset>", "</r>", "</pre>"));
      for (String tag : mmColorTags) {
        mmString = mmString.replace(tag, "");
      }
    }

    return mmString;
  }

  @Override
  public @NotNull Builder toBuilder() {
    return new BuilderImpl(this);
  }

  @ApiStatus.Internal
  static final class BuilderImpl implements Builder {

    private boolean gradients, hexColors, standardColors, legacyColors, advancedTransformations, blockCloseHex;
    private PlaceholderResolver placeholderResolver;
    private final Set<TextDecoration> removedTextDecorations;
    private final Set<NamedTextColor> removedColors;
    private int luminanceThreshold;

    @ApiStatus.Internal
    BuilderImpl() {
      this.gradients = true;
      this.hexColors = true;
      this.standardColors = true;
      this.legacyColors = false;
      this.advancedTransformations = false;
      this.blockCloseHex = true;
      this.placeholderResolver = PlaceholderResolver.empty();
      this.removedTextDecorations = new HashSet<>();
      this.removedColors = new HashSet<>();
      this.luminanceThreshold = 0;
    }

    @ApiStatus.Internal
    BuilderImpl(final MiniMessageWrapperImpl wrapper) {
      this.gradients = wrapper.gradients;
      this.hexColors = wrapper.hexColors;
      this.standardColors = wrapper.standardColors;
      this.legacyColors = wrapper.legacyColors;
      this.advancedTransformations = wrapper.advancedTransformations;
      this.blockCloseHex = wrapper.blockCloseHex;
      this.placeholderResolver = wrapper.placeholderResolver;
      this.removedTextDecorations = wrapper.removedTextDecorations;
      this.removedColors = wrapper.removedColors;
      this.luminanceThreshold = wrapper.luminanceThreshold;
    }

    @Override
    public @NotNull Builder gradients(final boolean parse) {
      this.gradients = parse;
      return this;
    }

    @Override
    public @NotNull Builder hexColors(final boolean parse) {
      this.hexColors = parse;
      return this;
    }

    @Override
    public @NotNull Builder standardColors(final boolean parse) {
      this.standardColors = parse;
      return this;
    }

    @Override
    public @NotNull Builder legacyColors(final boolean parse) {
      this.legacyColors = parse;
      return this;
    }

    @Override
    public @NotNull Builder advancedTransformations(final boolean parse) {
      this.advancedTransformations = parse;
      return this;
    }

    @Override
    public @NotNull Builder removeTextDecorations(final @NotNull TextDecoration... decorations) {
      this.removedTextDecorations.addAll(List.of(decorations));
      return this;
    }

    @Override
    public @NotNull Builder placeholderResolver(final @NotNull PlaceholderResolver placeholderResolver) {
      this.placeholderResolver = placeholderResolver;
      return this;
    }

    @Override
    public @NotNull Builder preventLuminanceBelow(int threshold) {
      this.luminanceThreshold = threshold;
      return this;
    }

    @Override
    public @NotNull Builder removeColors(final boolean blockCloseHex, final @NotNull NamedTextColor... colors) {
      this.blockCloseHex = blockCloseHex;
      this.removedColors.addAll(List.of(colors));
      return this;
    }

    @Override
    public @NotNull MiniMessageWrapper build() {
      return new MiniMessageWrapperImpl(this.gradients, this.hexColors, this.standardColors,
          this.legacyColors, this.advancedTransformations, this.blockCloseHex, this.placeholderResolver,
          this.removedTextDecorations, this.removedColors, this.luminanceThreshold);
    }
  }

  private double getLuma(@NotNull String color) {
    color = color.replaceAll("[&x#]", "");

    final int r = Integer.valueOf(color.substring(0, 2), 16);
    final int g = Integer.valueOf(color.substring(2, 4), 16);
    final int b = Integer.valueOf(color.substring(4, 6), 16);
    return (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
  }

  private @Nullable Character legacyCodeFromNamed(@NotNull NamedTextColor color) {
    if (BLACK.equals(color)) {
      return '0';
    } else if (DARK_BLUE.equals(color)) {
      return '1';
    } else if (DARK_GREEN.equals(color)) {
      return '2';
    } else if (DARK_AQUA.equals(color)) {
      return '3';
    } else if (DARK_RED.equals(color)) {
      return '4';
    } else if (DARK_PURPLE.equals(color)) {
      return '5';
    } else if (GOLD.equals(color)) {
      return '6';
    } else if (GRAY.equals(color)) {
      return '7';
    } else if (DARK_GRAY.equals(color)) {
      return '8';
    } else if (BLUE.equals(color)) {
      return '9';
    } else if (GREEN.equals(color)) {
      return 'a';
    } else if (AQUA.equals(color)) {
      return 'b';
    } else if (RED.equals(color)) {
      return 'c';
    } else if (LIGHT_PURPLE.equals(color)) {
      return 'd';
    } else if (YELLOW.equals(color)) {
      return 'e';
    } else if (WHITE.equals(color)) {
      return 'f';
    }
    return null;
  }

  public static final Map<Character, String> CHAR_COLORS = new ImmutableMap.Builder<Character, String>()
      .put('0', "<black>")
      .put('1', "<dark_blue>")
      .put('2', "<dark_green>")
      .put('3', "<dark_aqua>")
      .put('4', "<dark_red>")
      .put('5', "<dark_purple>")
      .put('6', "<gold>")
      .put('7', "<gray>")
      .put('8', "<dark_gray>")
      .put('9', "<blue>")
      .put('a', "<green>")
      .put('b', "<aqua>")
      .put('c', "<red>")
      .put('d', "<light_purple>")
      .put('e', "<yellow>")
      .put('f', "<white>")
      .put('k', "<obfuscated>")
      .put('l', "<bold>")
      .put('m', "<strikethrough>")
      .put('n', "<underlined>")
      .put('o', "<italic>")
      .put('r', "<reset>")
      .build();
}
