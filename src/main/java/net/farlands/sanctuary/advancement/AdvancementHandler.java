package net.farlands.sanctuary.advancement;

import dev.majek.advancements.Advancement;
import dev.majek.advancements.config.Frame;
import dev.majek.advancements.config.MinecraftFunction;
import dev.majek.advancements.config.Rewards;
import dev.majek.advancements.shared.ItemObject;
import dev.majek.advancements.trigger.ImpossibleTrigger;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AdvancementHandler extends Mechanic {

  private final Map<NamespacedKey, Advancement> advancementMap;

  public AdvancementHandler() {
    this.advancementMap = new HashMap<>();
  }

  @Override
  public void onStartup() {
    this.registerAdvancement(
        Advancement
            .builder(
                Objects.requireNonNull(NamespacedKey.fromString("farlands:nether/quenching-fire")),
                ItemObject.icon(Material.BLAZE_ROD),
                Component.text("Quenching Fire"),
                Component.text("Throw a fire resistance potion on a Blaze."),
                Map.of("impossible", new ImpossibleTrigger())
            )
            .makeChild(
                NamespacedKey.minecraft("nether/obtain_blaze_rod")
            )
            .rewards(
                Rewards.create().function(
                    MinecraftFunction.create(
                        Objects.requireNonNull(NamespacedKey.fromString("fl_rewards:nether/quenching-fire"))
                    ).addLine("give @s minecraft:blaze_rod 2")
                ).experience(100)
            )
            .toast(true)
            .announce(true)
            .frame(Frame.CHALLENGE)
            .build()
    );

    // This must be last
    Bukkit.reloadData();
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

  private void registerAdvancement(final @NotNull Advancement advancement) {
    Logging.log("Registered advancement " + advancement.key().asString());
    this.advancementMap.put(advancement.key(), advancement);
    advancement.activate(false);
  }
}
