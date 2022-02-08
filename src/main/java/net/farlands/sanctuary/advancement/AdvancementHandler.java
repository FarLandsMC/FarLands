package net.farlands.sanctuary.advancement;

import dev.majek.advancements.Advancement;
import dev.majek.advancements.config.Frame;
import dev.majek.advancements.config.MinecraftFunction;
import dev.majek.advancements.config.Rewards;
import dev.majek.advancements.shared.ItemObject;
import dev.majek.advancements.shared.Potion;
import dev.majek.advancements.trigger.ImpossibleTrigger;
import dev.majek.advancements.trigger.TickTrigger;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static net.kyori.adventure.text.format.NamedTextColor.*;


/**
 * Handles custom and rank advancements.
 */
public class AdvancementHandler extends Mechanic {

  private final Map<NamespacedKey, Advancement> advancementMap;

  /**
   * Instantiate :D
   * Should only be called in {@link FarLands} constructor.
   */
  public AdvancementHandler() {
    this.advancementMap = new HashMap<>();
  }

  /**
   * Register advancements and reload Bukkit data on server start.
   */
  @Override
  public void onStartup() {
    registerCustomAdvancements();
    registerRankAdvancements();

    Bukkit.reloadData(); // Do this once to load all advancements
  }

  /**
   * Get a custom advancement if it exists.
   *
   * @param key the advancement key
   * @return advancement
   */
  public @Nullable Advancement getAdvancement(final @NotNull NamespacedKey key) {
    return this.advancementMap.get(key);
  }

  /**
   * Grant a player an advancement.
   *
   * @param player the player
   * @param advancementKey the key of the advancement to grant
   */
  public void grantAdvancement(final @NotNull Player player, final @NotNull NamespacedKey advancementKey) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement grant "
        + player.getName() + " only " + advancementKey.asString());
  }

  /**
   * Check if a player has completed an advancement.
   *
   * @param player the player
   * @param advancementKey the key of the advancement to check
   * @return whether the advancement is complete
   */
  public boolean hasCompletedAdvancement(final @NotNull Player player, final @NotNull NamespacedKey advancementKey) {
    org.bukkit.advancement.Advancement advancement = Bukkit.getAdvancement(advancementKey);
    if (advancement == null) {
      throw new IllegalArgumentException("Unable to find advancement with key " + advancementKey.asString());
    }

    return player.getAdvancementProgress(advancement).isDone();
  }

  /**
   * Update advancements for a player based off of their rank.
   *
   * @param player the player
   * @param rank their rank
   */
  public void updateAdvancements(final @NotNull Player player, final @NotNull Rank rank) {
    // No advancements for these ranks since they're temporary
    if (rank == Rank.VOTER || rank == Rank.BIRTHDAY) {
      return;
    }

    // Give appropriate donation rank based on amount donated
    OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
    if (flp.amountDonated >= 60) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement grant "
          + player.getName() + " only farlands:ranks/sponsor");
    }
    if (flp.amountDonated >= 30) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement grant "
          + player.getName() + " only farlands:ranks/patron");
    }
    if (flp.amountDonated >= 10) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement grant "
          + player.getName() + " only farlands:ranks/donor");
    }

    // Grant all playtime ranks for staff and media
    if (rank == Rank.MEDIA || rank.isStaff()) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement grant "
          + player.getName() + " from farlands:ranks/initiate");
      return;
    }

    // Grant all playtime ranks for donor+
    if (rank.isDonationRank()) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement grant "
          + player.getName() + " from farlands:ranks/initiate");
      return;
    }

    // Remove all playtime and grant up to what they should have
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke "
        + player.getName() + " from farlands:ranks/initiate");
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement grant "
        + player.getName() + " until farlands:ranks/" + rank.toString().toLowerCase(Locale.ROOT));
  }

  private void registerAdvancement(final @NotNull Advancement advancement) {
    Logging.log("Registering advancement " + advancement.key().asString());
    this.advancementMap.put(advancement.key(), advancement);
    advancement.activate(false);
  }

  private void registerCustomAdvancements() {
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("nether/quenching-fire"),
                ItemObject.icon(Material.SPLASH_POTION).potion(Potion.FIRE_RESISTANCE),
                Component.text("Quenching Fire"),
                Component.text("Throw a fire resistance potion on a Blaze."),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(NamespacedKey.minecraft("nether/obtain_blaze_rod"))
            .rewards(
                Rewards.create().function(
                    MinecraftFunction.create(
                        Objects.requireNonNull(NamespacedKey.fromString("fl_rewards:nether/quenching-fire"))
                    ).addLine("give @s minecraft:blaze_rod 2") // TODO: 2/8/22 Change this - added for testing - majek
                ).experience(100)
            )
            .frame(Frame.CHALLENGE)
            .build()
    );
  }

  private void registerRankAdvancements() {
    JoinConfiguration joinConfig = JoinConfiguration.separator(Component.newline());

    // Root
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/root"),
                ItemObject.icon(Material.WHITE_BANNER),
                Component.text("FarLands Ranks", GOLD),
                Component.text("All of the ranks for FarLands", WHITE),
                new HashMap<>()
            )
            .makeRoot("block/netherite_block", true) // TODO: 2/8/22 Pick a suitable background texture
            .toast(false)
            .announce(false)
            .build()
    );

    // Playtime ranks head
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/playtime"),
                ItemObject.icon(Material.BLACK_BANNER),
                Component.text("Playtime Ranks", WHITE),
                Component.text("These ranks can be achieved by playing on the server.", WHITE),
                Map.of("auto", new TickTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/root"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Initiate
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/initiate"),
                ItemObject.icon(Material.LIGHT_GRAY_BANNER),
                Component.text("Initiate", GRAY),
                Component.text("1 home", WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/playtime"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Bard
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/bard"),
                ItemObject.icon(Material.YELLOW_BANNER),
                Component.text("Bard", YELLOW),
                ComponentUtils.join(joinConfig, "3 hours playtime", "3 homes", "/trade").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/initiate"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Esquire
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/esquire"),
                ItemObject.icon(Material.GREEN_BANNER),
                Component.text("Esquire", DARK_GREEN),
                ComponentUtils.join(joinConfig, "12 hours playtime", "5 homes", "/stack").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(Objects.requireNonNull(NamespacedKey.fromString("farlands:ranks/bard")))
            .toast(false)
            .announce(false)
            .build()
    );

    // Knight
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/knight"),
                ItemObject.icon(Material.ORANGE_BANNER),
                Component.text("Knight", GOLD),
                ComponentUtils.join(joinConfig, "1 day playtime", "8 homes",
                    "/package", "/ptime", "/sit").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/esquire"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Sage
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/sage"),
                ItemObject.icon(Material.LIGHT_BLUE_BANNER),
                Component.text("Sage", AQUA),
                ComponentUtils.join(joinConfig, "3 days playtime", "10 homes",
                    "/editsign", "/pweather", "/skull").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/knight"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Adept
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/adept"),
                ItemObject.icon(Material.LIME_BANNER),
                Component.text("Adept", GREEN),
                ComponentUtils.join(joinConfig, "6 days playtime", "12 homes",
                    "/colors", "/nick").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/sage"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Scholar
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/scholar"),
                ItemObject.icon(Material.BLUE_BANNER),
                Component.text("Scholar", BLUE),
                ComponentUtils.join(joinConfig, "1 week 3 days playtime", "16 homes").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/adept"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Donation ranks head
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/donation"),
                ItemObject.icon(Material.BLACK_BANNER),
                Component.text("Donation Ranks", WHITE),
                Component.text("These ranks can be achieved by donating to the server.", WHITE),
                Map.of("auto", new TickTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/root"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Donor
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/donor"),
                ItemObject.icon(Material.PINK_BANNER),
                Component.text("Donor", LIGHT_PURPLE),
                ComponentUtils.join(joinConfig, "10 USD", "24 homes", "/echest",
                    "/hat", "/hdb", "/particles").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/donation"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Patron
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/patron"),
                ItemObject.icon(Material.PURPLE_BANNER),
                Component.text("Patron", DARK_PURPLE),
                ComponentUtils.join(joinConfig, "30 USD", "32 homes",
                    "/craft", "/extinguish").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/donation"))
            .toast(false)
            .announce(false)
            .build()
    );

    // Sponsor
    this.registerAdvancement(
        Advancement
            .builder(
                FarLands.namespacedKey("ranks/sponsor"),
                ItemObject.icon(Material.CYAN_BANNER),
                Component.text("Sponsor", TextColor.color(0x32a4ea)),
                ComponentUtils.join(joinConfig, "60 USD", "40 homes", "/eat",
                    "/editarmorstand", "/kittycannon", "/mend", "/petblock", "/renameitem").color(WHITE),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(FarLands.namespacedKey("ranks/donation"))
            .toast(false)
            .announce(false)
            .build()
    );
  }
}
