package net.farlands.sanctuary.command;

import io.papermc.paper.advancement.AdvancementDisplay;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Data associated with plugin commands such as minimum rank to use, playtime required, etc.
 */
public class CommandData {

  private final @NotNull String name;
  private final @NotNull String description;
  private final @NotNull String usage;
  private @NotNull Category category;
  private @NotNull List<String> aliases;
  private boolean requiresAlias;
  private @NotNull Rank minimumRank;
  private @NotNull BooleanOperation rankCompare;
  private final @NotNull List<Advancement> advancementsRequired;
  private final @NotNull List<Material> craftedItemsRequired;
  private long playedHoursRequired;
  private @Nullable CustomRequirement customRequirement;

  /**
   * Simple command data. The bare minimum required.
   *
   * @param name command name
   * @param description command description
   * @param usage command usage
   * @return new command data
   */
  public static @NotNull CommandData simple(final @NotNull String name, final @NotNull String description,
                                            final @NotNull String usage) {
    return new CommandData(name, description, usage);
  }

  /**
   * Simple command data with the addition of a minimum rank to use.
   *
   * @param name command name
   * @param description command description
   * @param usage command usage
   * @param rank required rank to use the command
   * @return new command data
   */
  public static @NotNull CommandData withRank(final @NotNull String name, final @NotNull String description,
                                              final @NotNull String usage, final @NotNull Rank rank) {
    return new CommandData(name, description, usage).minimumRank(rank);
  }

  CommandData(final @NotNull String name, final @NotNull String description,
              final @NotNull String usage) {
    this.name = name;
    this.description = description;
    this.usage = usage;
    this.category = Category.STAFF;
    this.aliases = new ArrayList<>();
    this.requiresAlias = false;
    this.minimumRank = Rank.INITIATE;
    this.rankCompare = BooleanOperation.OR;
    this.advancementsRequired = new ArrayList<>();
    this.craftedItemsRequired = new ArrayList<>();
    this.playedHoursRequired = 0;
    this.customRequirement = null;
  }

  /**
   * Check if the sender has completed the required advancements to use the command.
   *
   * @param sender the command executor
   * @return advancements completion status
   */
  public boolean advancementsCompleted(final @NotNull CommandSender sender) {
    if (sender instanceof Player player) {
      return this.advancementsRequired.stream().allMatch(adv -> player.getAdvancementProgress(adv).isDone());
    }
    return true;
  }

  /**
   * Check if the sender has crafted the required items to use the command.
   *
   * @param sender the command executor
   * @return crafted items completion status
   */
  private boolean itemsCrafted(final @NotNull CommandSender sender) {
    UUID uuid;
    if (sender instanceof ConsoleCommandSender) {
      uuid = null;
    } else if (sender instanceof Player player) {
      uuid = player.getUniqueId();
    } else if (sender instanceof DiscordSender ds) {
      uuid = ds.getFlp().uuid;
    } else {
      uuid = null;
    }
    return uuid == null || this.craftedItemsRequired.stream().allMatch(mat ->
        Bukkit.getOfflinePlayer(uuid).getStatistic(Statistic.CRAFT_ITEM, mat) > 0);
  }

  /**
   * Check if the sender has completed the required playtime to use the command.
   *
   * @param sender the command executor
   * @return playtime completion status
   */
  private boolean playtimeCompleted(final @NotNull CommandSender sender) {
    return this.playedHoursRequired == 0
        || FarLands.getDataHandler().getOfflineFLPlayer(sender).secondsPlayed / 60 / 60 >= this.playedHoursRequired;
  }

  /**
   * Check if the sender has completed the custom requirement to use the command.
   *
   * @param sender the command executor
   * @return custom requirement completion status
   */
  private boolean customRequirementCompleted(final @NotNull CommandSender sender) {
    return this.customRequirement == null
        || this.customRequirement.complete(sender).getFirst();
  }

  /**
   * Check if the sender can use the command.
   *
   * @param sender the command executor
   * @return whether they can use the command
   */
  public boolean canUse(final @NotNull CommandSender sender) {
    if (sender instanceof ConsoleCommandSender) {
      return true;
    }
    OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

    if (this.minimumRank.isStaff() && sender instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
      return flp.rank.isStaff();
    }

    boolean rankMatches = flp.rank.specialCompareTo(this.minimumRank) >= 0;
    boolean everythingElse =
        this.advancementsCompleted(sender)
            && this.itemsCrafted(sender)
            && this.playtimeCompleted(sender)
            && this.customRequirementCompleted(sender);

    return this.rankCompare.apply(rankMatches, everythingElse);
  }

  /**
   * Whether this command has any requirements beyond rank.
   *
   * @return whether the command is rank only
   */
  public boolean rankOnly() {
    return this.advancementsRequired.isEmpty()
        && this.craftedItemsRequired.isEmpty()
        && this.playedHoursRequired == 0
        && this.customRequirement == null;
  }

  /**
   * Get a component listing the requirements to use the command.
   *
   * @return requirements list
   */
  public @NotNull Component getRequirements() {
    if (this.rankOnly()) {
      return ComponentColor.red("You must be at least rank ")
          .append(this.minimumRank.getLabel())
          .append(ComponentColor.red(" to use this command."));
    }

    TextComponent.Builder cBuilder = Component.text().color(NamedTextColor.RED)
        .content("You must meet the following requirements to run this command: \n");
    List<Component> requirements = new ArrayList<>();

    if (this.minimumRank != Rank.INITIATE) {
      requirements.add(
          Component.text("- At least rank ")
              .append(this.minimumRank.getLabel())
              .append(ComponentColor.red("\n%s", this.rankCompare.name()))
      );
    }

    if (!this.advancementsRequired.isEmpty()) {
      List<Component> advancements = this.advancementsRequired.stream().map(adv -> {
        AdvancementDisplay display = adv.getDisplay();
        if (display == null) {
          throw new IllegalArgumentException("Advancement is hidden and cannot be displayed.");
        }
        return ComponentUtils.hover(
            ComponentColor.green("[")
                .append(display.title())
                .append(Component.text("]")),
            display.description()
        );
      }).toList();

      requirements.add(
          Component.text("Complete the advancements: ").append(
              Component.join(
                  JoinConfiguration.separators(
                      Component.text(", "),
                      Component.text(", and ")
                  ),
                  advancements
              ))
      );
    }

    if (!this.craftedItemsRequired.isEmpty()) {
      List<Component> items = this.craftedItemsRequired.stream()
          .map(ItemStack::new).map(ComponentUtils::item).toList();
      requirements.add(
          Component.text("Craft the following items: ").append(Component.join(
                  JoinConfiguration.separators(
                      Component.text(", "),
                      Component.text(", and ")
                  ),
                  items
              )
          ));
    }

    if (this.playedHoursRequired > 0) {
      requirements.add(Component.text("Play more than " + this.playedHoursRequired
          + " hour" + (this.playedHoursRequired != 1 ? "s" : "")));
    }

    if (this.customRequirement != null) {
      requirements.add(this.customRequirement.complete(null).getSecond());
    }

    return cBuilder.append(
        Component.join(JoinConfiguration.separator(Component.text("\n- ")), requirements)
    ).build();
  }

  /**
   * Send the requirement to use the command to the sender.
   *
   * @param sender the command executor
   */
  public void sendRequirements(final @NotNull CommandSender sender) {
    sender.sendMessage(this.getRequirements());
  }

  /*
   * Getters and setters
   */

  /**
   * Command name.
   *
   * @return name
   */
  public @NotNull String name() {
    return this.name;
  }

  /**
   * Command description.
   *
   * @return description.
   */
  public @NotNull String description() {
    return this.description;
  }

  /**
   * Command usage.
   *
   * @return usage.
   */
  public @NotNull String usage() {
    return this.usage;
  }

  /**
   * Command category. Used in /help.
   *
   * @return category
   */
  public @NotNull Category category() {
    return this.category;
  }

  /**
   * Set the command category.
   *
   * @param category the category
   * @return this data
   */
  public @NotNull CommandData category(final @NotNull Category category) {
    this.category = category;
    return this;
  }

  /**
   * Command aliases.
   *
   * @return aliases
   */
  public @NotNull List<String> aliases() {
    return this.aliases;
  }

  /**
   * Set the command aliases.
   *
   * @param aliases the aliases
   * @return this data
   */
  public @NotNull CommandData aliases(final @NotNull List<String> aliases) {
    this.aliases = aliases;
    return this;
  }

  /**
   * Add aliases to the command. Set whether the command requires the alias to be passed through.
   *
   * @param requiresAlias whether the alias should be passed with the args
   * @param aliases the aliases
   * @return this data
   */
  public @NotNull CommandData aliases(final boolean requiresAlias, final @NotNull String... aliases) {
    this.requiresAlias = requiresAlias;
    this.aliases.addAll(Arrays.stream(aliases).toList());
    return this;
  }

  /**
   * Whether the alias should be passed with the arguments.
   * Needed in things like {@link net.farlands.sanctuary.command.player.CommandShrug}.
   *
   * @return requires alias
   */
  public boolean requiresAlias() {
    return this.requiresAlias;
  }

  /**
   * The rank required to use the command.
   *
   * @return minimum rank
   */
  public @NotNull Rank minimumRank() {
    return this.minimumRank;
  }

  /**
   * Set the rank required to use the command.
   *
   * @param minimumRank minimum rank
   * @return this data
   */
  public @NotNull CommandData minimumRank(final @NotNull Rank minimumRank) {
    this.minimumRank = minimumRank;
    return this;
  }

  /**
   * <p>Get the boolean operation that will be used when evaluating if a sender can use the command.</p>
   * <p>If the operation is OR, then the sender will be able to use the command if they have the
   * required rank or the necessary advancements, items crafted, etc.</p>
   * <p>If the operation is AND, then the sender will need to have the minimum rank and all the
   * other necessary advancements, items crafted, etc.</p>
   *
   * @return boolean operation used in rank comparison
   */
  public @NotNull BooleanOperation rankCompare() {
    return this.rankCompare;
  }

  /**
   * <p>Set the boolean operation that will be used when evaluating if a sender can use the command.</p>
   * <p>If the operation is OR, then the sender will be able to use the command if they have the
   * required rank or the necessary advancements, items crafted, etc.</p>
   * <p>If the operation is AND, then the sender will need to have the minimum rank and all the
   * other necessary advancements, items crafted, etc.</p>
   *
   * @param rankCompare boolean operation used in rank comparison
   * @return this data
   */
  public @NotNull CommandData rankCompare(final @NotNull BooleanOperation rankCompare) {
    this.rankCompare = rankCompare;
    return this;
  }

  /**
   * The advancements required to use the command.
   *
   * @return required advancements
   */
  public @NotNull List<Advancement> advancementsRequired() {
    return this.advancementsRequired;
  }

  /**
   * Add advancements that must be completed before the command can be used.
   *
   * @param advancementNames the keys for the advancements
   * @return this data
   */
  public @NotNull CommandData advancementsRequired(final @NotNull String... advancementNames) {
    this.advancementsRequired.addAll(Arrays.stream(advancementNames).map(name ->
        Bukkit.getAdvancement(NamespacedKey.minecraft(name))).toList());
    return this;
  }

  /**
   * The items the sender must craft before being able to use the command.
   *
   * @return required items that must be crafted
   */
  public @NotNull List<Material> craftedItemsRequired() {
    return this.craftedItemsRequired;
  }

  /**
   * Add items that must be crafted before the command can be executed.
   *
   * @param materials items to be crafted
   * @return this data
   */
  public @NotNull CommandData craftedItemsRequired(final @NotNull Material... materials) {
    this.craftedItemsRequired.addAll(Arrays.stream(materials).toList());
    return this;
  }

  /**
   * The playtime, in hours, required to be able to use the command.
   *
   * @return playtime hours required
   */
  public long playedHoursRequired() {
    return this.playedHoursRequired;
  }

  /**
   * Set the hours that must be played before the command can be executed.
   *
   * @param playedHoursRequired playtime hours required
   * @return this data
   */
  public @NotNull CommandData playedHoursRequired(final @Range(from = 0, to = Long.MAX_VALUE) long playedHoursRequired) {
    this.playedHoursRequired = playedHoursRequired;
    return this;
  }

  /**
   * The custom requirement to use the command.
   *
   * @return custom requirement
   */
  public @Nullable CustomRequirement customRequirement() {
    return this.customRequirement;
  }

  /**
   * Create a custom requirement for using the command. This allows you to specify a custom
   * message to be displayed if the criteria hasn't been met.
   *
   * @param customRequirement custom requirement
   * @return this data.
   */
  public @NotNull CommandData customRequirement(final @NotNull CustomRequirement customRequirement) {
    this.customRequirement = customRequirement;
    return this;
  }

  /**
   * Define a boolean operation: AND/OR.
   */
  public enum BooleanOperation {
    OR(Boolean::logicalOr),
    AND(Boolean::logicalAnd);

    private final BiFunction<Boolean, Boolean, Boolean> operation;

    BooleanOperation(BiFunction<Boolean, Boolean, Boolean> operation) {
      this.operation = operation;
    }

    /**
     * Apply the boolean operation.
     *
     * @param a first boolean
     * @param b second boolean
     * @return whether the application was successful
     */
    public boolean apply(boolean a, boolean b) {
      return this.operation.apply(a, b);
    }
  }
}
