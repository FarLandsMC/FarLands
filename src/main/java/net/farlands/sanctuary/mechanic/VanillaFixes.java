package net.farlands.sanctuary.mechanic;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.Region;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.EnumFilter;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FarLands custom fixes for Vanilla bugs.
 */
public class VanillaFixes extends Mechanic {

    /**
     * Prevent kelp growth into flowing water
     * <br>
     * https://bugs.mojang.com/browse/MC-133354
     */
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        Block block = event.getBlock();
        switch (event.getNewState().getType()) {
            case KELP, KELP_PLANT -> {
                /*
                 * Water Level Meanings:
                 * 0: Source Block
                 * 1-7: Flowing Water
                 * 8-15: Falling Water
                 */
                if (block.getType() != Material.WATER) return;
                Levelled lvld = (Levelled) block.getBlockData();
                // Only block flowing water, so the "trick" with placing kelp to fill a water elevator still works
                if (lvld.getLevel() >= 1 && lvld.getLevel() <= 7) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Allow moving overstacked(more than item stack limit) items
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        Inventory inv = event.getClickedInventory();
        Player player = Bukkit.getPlayer(event.getWhoClicked().getUniqueId());
        if (
            player == null
            || player.getGameMode() == GameMode.CREATIVE // This gets funky with creative players, who likely don't need it, so don't bother
            || cursor == null || current == null || inv == null
        ) {
            return;
        }

        boolean cursorOverstacked = cursor.getAmount() > cursor.getMaxStackSize();

        if (cursor.getMaxStackSize() == 64 || current.getMaxStackSize() == 64) return; // Not overstackable items

        if (event.getAction() == InventoryAction.PLACE_ALL) { // PLACE_ALL is when placing an entire stack (left click default)
            if (cursorOverstacked && event.getClickedInventory() != null) {
                event.getView().setCursor(null);
                event.setCurrentItem(null);
                event.getClickedInventory().setItem(event.getSlot(), cursor.clone());
                event.setCancelled(true);
            }
        }
    }

    /**
     * Notify about pets left behind or teleport them to the player.
     *
     * @param event PlayerTeleportEvent
     */
    @EventHandler
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        final Location from = event.getFrom();
        final Location destination = event.getTo();
        final Region claim = RegionProtection.getDataManager().getHighestPriorityRegionAt(from);

        if (from.getWorld().equals(destination.getWorld()) && from.distanceSquared(destination) < 100) {
            return; // Less than 10 block distance doesn't need to warn
        }

        // Get all nearby pets from the teleport location
        final Collection<Tameable> nearbyPets = from.getNearbyLivingEntities(20.0)
            .stream()
            .filter(animal -> animal instanceof Tameable)
            .map(animal -> (Tameable) animal)
            .filter(pet -> pet.getOwnerUniqueId() != null && pet.getOwnerUniqueId().equals(player.getUniqueId()))
            .toList();

        // Try to teleport standing pets
        AtomicInteger failedToTeleport = new AtomicInteger();
        nearbyPets.forEach(pet -> {
            // Sittable pets (dogs, cats, and parrots) are the only pets that are supposed to tp follow in vanilla
            if (pet instanceof Sittable sittablePet) {
                if (!sittablePet.isSitting()) {
                    if (!teleport(pet, destination.add(1.0, 1.0, 1.0))) {
                        failedToTeleport.getAndIncrement();
                    }
                }
            }
        });

        // Notify about pets that failed to teleport due to region flags
        if (failedToTeleport.get() > 0) {
            player.sendMessage(
                ComponentColor.gold(
                    "You teleported to a region {} of your pets cannot teleport to! They were left at {:aqua}.",
                    failedToTeleport.get(),
                    FLUtils.toSimpleString(from)
                )
            );
        }

        // Check for pets left sitting in the wild or another player's claim
        if (claim == null || !claim.<TrustMeta>getAndCreateFlagMeta(RegionFlag.TRUST).hasTrust(player, TrustLevel.BUILD, claim)) {
            AtomicInteger leftBehind = new AtomicInteger();
            nearbyPets.forEach(pet -> {
                if (pet instanceof Sittable sittablePet) {
                    if (sittablePet.isSitting()) {
                        leftBehind.getAndIncrement();
                    }
                }
            });

            // Notify about pets left behind
            if (leftBehind.get() > 0) {
                player.sendMessage(
                    ComponentColor.gold(
                        "You left {} of your pets sitting at {:aqua}.",
                        leftBehind.get(),
                        FLUtils.toSimpleString(from)
                    )
                );
            }
        }
    }

    /**
     * Try to teleport an entity. This will return true if the teleport was successful and false if it failed.
     *
     * @param entity      the entity to teleport
     * @param destination the location to teleport to
     * @return success status
     */
    private boolean teleport(final @NotNull LivingEntity entity, final @NotNull Location destination) {
        final FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(destination);
        if (
            flags.<EnumFilter.EntityFilter>getFlagMeta(RegionFlag.DENY_ENTITY_TELEPORT).isBlocked(entity.getType()) ||
            !flags.isAllowed(RegionFlag.FOLLOW)
        ) {
            return false;
        } else {
            entity.teleport(destination);
            return true;
        }
    }

    /**
     * Make it such that one can till Mycelium and Podzol blocks
     * <p>
     * https://bugs.mojang.com/browse/MC-8231
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (
            event.hasBlock()
            && event.hasItem()
            && action.isRightClick()
            && event.getItem().getType().name().endsWith("_HOE")
            && Set.of(Material.MYCELIUM, Material.PODZOL).contains(event.getClickedBlock().getType())
        ) {
            FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(event.getClickedBlock().getLocation());
            if (!(flags == null || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(event.getPlayer(), TrustLevel.BUILD, flags))) {
                event.getPlayer().sendMessage(ComponentColor.red("You cannot use that here."));
                return;
            }

            event.getClickedBlock().setType(Material.FARMLAND);
            event.getPlayer().swingHand(event.getHand() == null ? EquipmentSlot.HAND : event.getHand());
            event.getPlayer().playSound(
                event.getClickedBlock().getLocation(),
                Sound.ITEM_HOE_TILL,
                SoundCategory.BLOCKS,
                1,
                1
            );
            FLUtils.damageItem(event.getItem(), 1);
        }

    }
}
