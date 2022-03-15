package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.EvidenceLocker;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Gui for player evidence lockers.
 */
public class GuiEvidenceLocker extends Gui {

    private final OfflineFLPlayer flp;
    private final EvidenceLocker  locker;

    private int currentPunishment;

    public GuiEvidenceLocker(OfflineFLPlayer flp) {
        super(flp.punishments.get(0).asComponent(0), 54);
        this.flp = flp;
        this.locker = FarLands.getDataHandler().getEvidenceLocker(flp);
        this.currentPunishment = 0;
    }

    /**
     * Open the GUI for the player and open the evidence locker
     */
    @Override
    public void openGui(Player player) {
        super.openGui(player);
        FarLands.getDataHandler().openEvidenceLocker(this.flp.uuid);
    }

    /**
     * Save current inventory into the player's evidence locker
     */
    private void saveInventory() {
        List<ItemStack> subLocker = this.locker.getSubLocker(this.flp.punishments.get(this.currentPunishment));
        subLocker.clear();
        for (int i = 0; i < 54; i++) {
            if (this.clickActions.containsKey(i)) {
                subLocker.add(null);
            } else {
                subLocker.add(clone(this.inventory.getItem(i)));
            }
        }
    }

    /**
     * Change the inventory to a new EvidenceLocker page
     * @param move The page delta
     */
    private void changeInventory(int move) {
        saveInventory();
        this.currentPunishment += move;
        newInventory(54, this.flp.punishments.get(this.currentPunishment).asComponent(this.currentPunishment));
    }

    @Override
    protected void populateInventory() {
        List<ItemStack> subLocker = this.locker.getSubLocker(this.flp.punishments.get(this.currentPunishment));
        for (int i = 0; i < subLocker.size(); i++) { // Show the contents of the player's locker
            this.inventory.setItem(i, clone(subLocker.get(i)));
        }

        if (this.currentPunishment < this.flp.punishments.size() - 1) { // Next page button
            addActionItem(53, Material.EMERALD_BLOCK, ComponentColor.gold("Next").decorate(TextDecoration.BOLD), () -> changeInventory(1));
        } else {
            addLabel(53, Material.REDSTONE_BLOCK, ComponentColor.red("No Next Locker").decorate(TextDecoration.BOLD));
        }

        if (this.currentPunishment > 0) { // Previous page button
            addActionItem(45, Material.EMERALD_BLOCK, ComponentColor.gold("Previous").decorate(TextDecoration.BOLD), () -> changeInventory(-1));
        } else {
            addLabel(45, Material.REDSTONE_BLOCK, ComponentColor.red("No Previous Locker").decorate(TextDecoration.BOLD));
        }
    }

    @Override
    protected void onClose() {
        saveInventory();
        FarLands.getDataHandler().saveEvidenceLockers();
        FarLands.getDataHandler().closeEvidenceLocker(this.flp.uuid);
    }
}
