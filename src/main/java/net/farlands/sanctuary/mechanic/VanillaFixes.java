package net.farlands.sanctuary.mechanic;

import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandStack;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * FarLands custom fixes for Vanilla bugs.
 */
public class VanillaFixes extends Mechanic {

    @EventHandler
    // Prevent kelp growth into flowing water
    // https://bugs.mojang.com/browse/MC-133354
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

    // Allow overstacking in inventory and moving overstacked items
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        Inventory inv = event.getClickedInventory();
        Player player = Bukkit.getPlayer(event.getWhoClicked().getUniqueId());
        boolean isEsquire = FarLands.getDataHandler().getOfflineFLPlayer(player)
            .rank
            .compareTo(
                FarLands.getCommandHandler().getCommand(CommandStack.class).getMinRankRequirement()
            ) > 0;
        // This gets funky with creative players, who likely don't need it, so don't bother
        if (
            player.getGameMode() == GameMode.CREATIVE ||
                cursor == null || current == null || inv == null
        ) {
            return;
        }

        boolean cursorOverstacked = cursor.getAmount() > cursor.getMaxStackSize();

        if (cursor.getMaxStackSize() == 64 || current.getMaxStackSize() == 64) return;

        switch (event.getAction()) {
            case PLACE_ALL -> { // Allow movement of overstacked items within inventory
                if (cursorOverstacked && event.getClickedInventory() != null) {
                    event.getView().setCursor(null);
                    event.setCurrentItem(null);
                    event.getClickedInventory().setItem(event.getSlot(), cursor.clone());
                    event.setCancelled(true);
                }
            }
            case NOTHING, PICKUP_SOME, PICKUP_ONE, PLACE_SOME -> { // Allow stacking within inventory
                if (!cursor.isSimilar(current) || cursor.getType() == Material.AIR || current.getType() == Material.AIR) {
                    return;
                }
                if (!isEsquire) return;
                int amt = current.getAmount() + cursor.getAmount();
                if (event.getSlotType() == InventoryType.SlotType.RESULT) return; // Crafting Table/Furnace/Etc. output
                if (amt > cursor.getMaxStackSize() && amt < 128 && current.getAmount() < 64) {

                    if (amt <= 64) {
                        event.getView().setCursor(null);
                    }

                    event.setCurrentItem(null);
                    ItemStack item = cursor.clone();
                    item.setAmount(Math.min(amt, 64));
                    inv.setItem(event.getSlot(), item);

                    if (amt > 64) {
                        ItemStack newCursor = cursor.clone();
                        newCursor.setAmount(amt - 64);
                        event.getView().setCursor(newCursor);
                    }
                }
            }
            case COLLECT_TO_CURSOR -> { // Allow stacking within the inventory
                if (cursor.getMaxStackSize() >= 64 || !isEsquire) return;
                int count = cursor.getAmount();

                List<Pair<Integer, ItemStack>> validSlots = new ArrayList<>();
                for (int slot = 0; slot < inv.getContents().length; slot++) {
                    ItemStack stack = inv.getContents()[slot];
                    if (stack != null && stack.isSimilar(cursor)) {
                        validSlots.add(new Pair<>(slot, stack));
                    }
                }

                int totalInInv = validSlots.stream().map(Pair::getSecond).map(ItemStack::getAmount).reduce(0, Integer::sum);

                if (totalInInv <= 64 - count) {
                    validSlots.forEach((p) -> inv.setItem(p.getFirst(), null));
                    cursor.setAmount(totalInInv + count);

                } else {
                    int c = cursor.getAmount();
                    for (Pair<Integer, ItemStack> p : validSlots) {
                        ItemStack stack = p.getSecond();
                        if (c + stack.getAmount() <= 64) {
                            c += stack.getAmount();
                            inv.setItem(p.getFirst(), null);
                        } else {
                            p.getSecond().setAmount(p.getSecond().getAmount() - (64 - c));
                            c = 64;
                            break;
                        }
                    }
                    cursor.setAmount(c);
                }
            }
        }
    }
}
