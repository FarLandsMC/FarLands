package net.farlands.sanctuary.mechanic;

import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.CustomRequirement;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class QuenchingFireAdvancement extends Mechanic implements CustomRequirement {

  /**
   * Used for Quenching Fire advancement.
   */
  private static final Set<Player> hasThrownPotionRecently = new HashSet<>();
  private static final @NotNull NamespacedKey quenchingFireKey = Objects.requireNonNull(NamespacedKey.fromString("farlands:quenching-fire"));

  /**
   * Check if the sender has completed the advancement
   *
   * @param sender the sender executing the command
   * @return a pair containing the completion status and the message detailing how to complete the advancement
   */
  @Override
  public @NotNull Pair<@Nullable Boolean, @Nullable Component> complete(@Nullable CommandSender sender) {
    if (sender instanceof Player player) {
      return new Pair<>(
          player.getPersistentDataContainer().has(quenchingFireKey, PersistentDataType.INTEGER),
          Component.text("You must throw a fire resistance potion on a Blaze.")
      );
    } else {
      return new Pair<>(true, Component.text("You must throw a fire resistance potion on a Blaze."));
    }
  }

  /**
   * Check for a player throwing a fire resistance potion.
   *
   * @param event the event
   */
  @EventHandler
  public void potionThrow(PlayerInteractEvent event) {
    if (
        (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
            && event.getItem() != null && event.getItem().getType() == Material.SPLASH_POTION
            && event.getItem().getItemMeta() instanceof PotionMeta meta
            && !event.getPlayer().getPersistentDataContainer().has(quenchingFireKey, PersistentDataType.INTEGER)
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
  public void blazeNoBlazeAnymore(EntityPotionEffectEvent event) {
    if (
        event.getCause() == EntityPotionEffectEvent.Cause.POTION_SPLASH
            && event.getEntity().getType() == EntityType.BLAZE
            && event.getNewEffect() != null
            && event.getModifiedType().equals(PotionEffectType.FIRE_RESISTANCE)
    ) {

      List<Player> players = event.getEntity().getLocation().getNearbyEntities(10.0, 100.0, 10.0)
          .stream()
          .filter(entity -> entity instanceof Player)
          .map(entity -> (Player) entity)
          .filter(hasThrownPotionRecently::contains)
          .filter(player -> !player.getPersistentDataContainer().has(quenchingFireKey, PersistentDataType.INTEGER))
          .toList();

      // In theory this is one player - I'm sure someone will break it
      players.forEach(player -> {
        player.getPersistentDataContainer().set(quenchingFireKey, PersistentDataType.INTEGER, 1);

        player.sendMessage(ComponentColor.green("You can now use ")
            .append(ComponentUtils.command("/ext")).append(Component.text("!")));
        player.giveExp(100);
        player.getLocation().getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 6.0F, 1.0F);

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
        Logging.broadcastIngame(
            flp.getDisplayName().append(Component.empty().color(NamedTextColor.WHITE)
                .append(Component.text(" has completed the FarLands challenge "))
                .append(Component.text("[Quenching Fire]", NamedTextColor.GOLD)
                    .hoverEvent(HoverEvent.showText(
                        Component.empty().color(NamedTextColor.GOLD)
                            .append(Component.text("Quenching Fire (Custom FarLands Advancement)"))
                            .append(Component.newline())
                            .append(Component.text("Throw a fire resistance potion on a Blaze."))
                    )))), true
        );

        hasThrownPotionRecently.remove(player);
      });
    }
  }
}
