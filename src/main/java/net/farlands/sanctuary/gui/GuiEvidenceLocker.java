package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.EvidenceLocker;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GuiEvidenceLocker extends Gui {
    private final OfflineFLPlayer flp;
    private final EvidenceLocker locker;
    private int currentPunishment;

    public GuiEvidenceLocker(OfflineFLPlayer flp) {
        super("Evidence Locker", flp.punishments.get(0).toFormattedString(0), 54);
        this.flp = flp;
        this.locker = FarLands.getDataHandler().getEvidenceLocker(flp);
        this.currentPunishment = 0;
    }

    @Override
    public void openGui(Player player) {
        super.openGui(player);
        FarLands.getDataHandler().openEvidenceLocker(flp.uuid);
    }

    private void saveInventory() {
        List<ItemStack> subLocker = locker.getSubLocker(flp.punishments.get(currentPunishment));
        subLocker.clear();
        for(int i = 0;i < 54;++ i) {
            if(clickActions.containsKey(i))
                subLocker.add(null);
            else
                subLocker.add(clone(inv.getItem(i)));
        }
    }

    private void changeInventory(int move) {
        saveInventory();
        currentPunishment += move;
        newInventory(54, flp.punishments.get(currentPunishment).toFormattedString(currentPunishment));
    }

    @Override
    protected void populateInventory() {
        List<ItemStack> subLocker = locker.getSubLocker(flp.punishments.get(currentPunishment));
        for(int i = 0;i < subLocker.size();++ i)
            inv.setItem(i, clone(subLocker.get(i)));

        if(currentPunishment < flp.punishments.size() - 1)
            addActionItem(53, Material.EMERALD_BLOCK, ChatColor.GOLD.toString() + ChatColor.BOLD + "Next", () -> changeInventory(1));
        else
            addLabel(53, Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "No Next Locker");

        if(currentPunishment > 0)
            addActionItem(45, Material.EMERALD_BLOCK, ChatColor.GOLD.toString() + ChatColor.BOLD + "Previous", () -> changeInventory(-1));
        else
            addLabel(45, Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "No Previous Locker");
    }

    @Override
    protected void onClose() {
        saveInventory();
        FarLands.getDataHandler().saveEvidenceLockers();
        FarLands.getDataHandler().closeEvidenceLocker(flp.uuid);
    }
}
