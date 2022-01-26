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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * An object used for checking if players match the required things before running a command.
 * <br>
 * By default, these all act with AND, though it can be changed so that the Rank can override.
 * <br><br>
 * <bold>** Rank Requirements only work for player senders - if not a Player, then it will default to true **</bold>
 * <br><br>
 * By using<br>
 * <code>
 * CommandRequirement.builder()<br>
 * .rank(Rank.KNIGHT)<br>
 * .advancements("end/find_end_city")<br>
 * .itemsCrafted(Material.ENDER_CHEST)<br>
 * .playtimeHours(48)<br>
 * .build()<br>
 * </code>
 * It will compare with the player having Knight Rank AND "end/find_end_city" advancement AND crafted and Ender Chest AND played more than 48 hours
 */
public final class CommandRequirement {

    public static CommandRequirement.Builder builder() {
        return new Builder();
    }

    public static CommandRequirement rank(Rank rank) {
        return builder().rank(rank).build();
    }

    private final Builder builder;

    public CommandRequirement(Builder builder) {
        this.builder = builder;
    }

    public boolean matchesAdvancements(CommandSender sender) {
        if (sender instanceof Player player) {
            return this.builder.advancements == null
                   || this.builder.advancements.stream().allMatch(adv -> player.getAdvancementProgress(adv).isDone());
        }
        return true;
    }

    private boolean matchesItems(CommandSender sender) {
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
        return uuid == null
               || this.builder.itemsCrafted == null
               || this.builder.itemsCrafted.stream().allMatch(
            mat -> Bukkit.getOfflinePlayer(uuid)
                       .getStatistic(Statistic.CRAFT_ITEM, mat) > 0
        );
    }

    private boolean matchesPlaytime(OfflineFLPlayer flp) {
        return this.builder.playtimeHours == 0
               || flp.secondsPlayed / 60 / 60 >= this.builder.playtimeHours;
    }

    private boolean matchesPredicate(OfflineFLPlayer flp) {
        return this.builder.customPredicate == null
               || this.builder.customPredicate.test(flp);
    }

    public boolean matches(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if(this.builder.staff) {
            return flp.rank.isStaff();
        }

        boolean rankMatches = flp.rank.specialCompareTo(builder.rank) >= 0;
        boolean everythingElse =
            this.matchesAdvancements(sender)
            && this.matchesItems(sender)
            && this.matchesPlaytime(flp)
            && this.matchesPredicate(flp);

        return this.builder.rankCompare.apply(rankMatches, everythingElse);
    }

    public Rank rank() {
        return this.builder.rank;
    }

    public boolean rankOnly() {
        return this.builder.advancements == null
               && this.builder.itemsCrafted == null
               && this.builder.playtimeHours == 0
               && this.builder.customPredicate == null;
    }

    public void sendRequirements(CommandSender sender) {
        if (this.rankOnly()) {
            sender.sendMessage(
                ComponentColor.red("You must be at least rank ")
                    .append(this.builder.rank.getLabel())
                    .append(ComponentColor.red(" to use this command."))
            );
            return;
        }
        TextComponent.Builder cBuilder = Component.text().color(NamedTextColor.RED).content("You must have the following requirements to run this command: \n");
        List<Component> reqs = new ArrayList<>();

        if (this.builder.rank != Rank.INITIATE) {
            reqs.add(
                Component.text("- At least rank ")
                    .append(this.builder.rank.getLabel())
                    .append(ComponentColor.red("\n%s", this.builder.rankCompare.name()))
            );
        }

        if (this.builder.advancements != null) {
            List<Component> advs = this.builder.advancements.stream().map(adv -> {
                AdvancementDisplay disp = adv.getDisplay();
                return ComponentUtils.hover(
                    ComponentColor.green("[")
                        .append(disp.title())
                        .append(Component.text("]")),
                    disp.description()
                );
            }).toList();

            reqs.add(
                Component.text("The Advancements: ").append(
                    Component.join(
                        JoinConfiguration.separators(
                            Component.text(", "),
                            Component.text(", and ")
                        ),
                        advs
                    ))
            );
        }

        if (this.builder.itemsCrafted != null) {
            List<Component> items = this.builder.itemsCrafted.stream().map(ItemStack::new).map(ComponentUtils::item).toList();
            reqs.add(
                Component.text("Crafted Items: ").append(Component.join(
                    JoinConfiguration.separators(
                        Component.text(", "),
                        Component.text(", and ")
                    ),
                    items
                )
            ));
        }

        if (this.builder.playtimeHours > 0) {
            reqs.add(Component.text("Play more than " + this.builder.playtimeHours + " hour" + (this.builder.playtimeHours != 1 ? "s" : "")));
        }

        cBuilder.append(Component.join(JoinConfiguration.separator(Component.text("\n- ")), reqs));
        sender.sendMessage(cBuilder.build());


    }

    public static class Builder {

        private Rank                       rank            = Rank.INITIATE;
        private boolean                    staff           = false;
        private BooleanOperation           rankCompare     = BooleanOperation.OR;
        private List<Advancement>          advancements    = null;
        private List<Material>             itemsCrafted    = null;
        private long                       playtimeHours   = 0;
        private Predicate<OfflineFLPlayer> customPredicate = null;

        public Builder rank(@NotNull Rank rank) {
            this.rank = rank;
            if (this.rank.isStaff()) this.staff = true;
            return this;
        }

        public Builder staff(boolean isStaff) {
            this.staff = isStaff;
            return this;
        }

        public Builder rankCompare(@NotNull BooleanOperation rankCompare) {
            this.rankCompare = rankCompare;
            return this;
        }

        public Builder itemsCrafted(@NotNull Material... items) {
            this.itemsCrafted = List.of(items);
            return this;
        }

        public Builder advancement(@NotNull String... advancements) {
            List<String> advs = List.of(advancements);
            this.advancements = advs.stream().map(s -> Bukkit.getAdvancement(NamespacedKey.minecraft(s))).toList();
            return this;
        }

        public Builder playtimeHours(@Range(from = 0, to = Long.MAX_VALUE) long amount) {
            this.playtimeHours = amount;
            return this;
        }

        public Builder customPredicate(@NotNull Predicate<OfflineFLPlayer> predicate) {
            this.customPredicate = predicate;
            return this;
        }

        public CommandRequirement build() {
            return new CommandRequirement(this);
        }

    }

    public enum BooleanOperation {
        OR(Boolean::logicalOr),
        AND(Boolean::logicalAnd),
        ;

        private final BiFunction<Boolean, Boolean, Boolean> operation;

        BooleanOperation(BiFunction<Boolean, Boolean, Boolean> operation) {

            this.operation = operation;
        }

        public boolean apply(boolean a, boolean b) {
            return this.operation.apply(a, b);
        }
    }
}
