package net.farlands.sanctuary.advancement;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Custom requirement for `/ext` that requires throwing a fire resistance potion on a Blaze.
 */
public class QuenchingFire extends Mechanic {

  /**
   * Keep track of players who have recently thrown potions.
   */
  private static final @NotNull Set<Player> hasThrownPotionRecently = new HashSet<>();

  /**
   * Check for a player throwing a fire resistance potion.
   *
   * @param event the event
   */
  @EventHandler
  public void potionThrow(@NotNull PlayerInteractEvent event) {
    if (
        (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
            && event.getItem() != null && event.getItem().getType() == Material.SPLASH_POTION
            && event.getItem().getItemMeta() instanceof PotionMeta meta
    ) {
      if (meta.getBasePotionData().getType() == PotionType.FIRE_RESISTANCE) {
        hasThrownPotionRecently.add(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(
            FarLands.getInstance(),
            () -> hasThrownPotionRecently.remove(event.getPlayer()),
            100L
        );
      }
    }
  }

  /**
   * Check for a blaze being affected by a fire resistance potion.
   *
   * @param event the event
   */
  @EventHandler
  public void blazeNoBlazeAnymore(@NotNull EntityPotionEffectEvent event) {
    if (
        event.getCause() == EntityPotionEffectEvent.Cause.POTION_SPLASH
            && event.getEntity().getType() == EntityType.BLAZE
            && event.getNewEffect() != null
            && event.getModifiedType().equals(PotionEffectType.FIRE_RESISTANCE)
    ) {

      final @NotNull NamespacedKey quenchingFire = Objects.requireNonNull(NamespacedKey.fromString("farlands:nether/quenching-fire"));

      final List<Player> players = event.getEntity().getLocation().getNearbyEntities(10.0, 100.0, 10.0)
          .stream()
          .filter(entity -> entity instanceof Player)
          .map(entity -> (Player) entity)
          .filter(hasThrownPotionRecently::contains)
          .filter(player -> !FarLands.getAdvancementHandler().hasCompletedAdvancement(player, quenchingFire))
          .toList();

      // In theory this is one player - I'm sure someone will break it
      players.forEach(player -> {
        player.sendMessage(ComponentColor.green("You can now use ")
            .append(ComponentUtils.command("/ext")).append(Component.text("!")));
        FarLands.getAdvancementHandler().grantAdvancement(player, quenchingFire);
        hasThrownPotionRecently.remove(player);
      });
    }
  }
}
