package net.farlands.odyssey.gui;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.struct.Punishment;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagList;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GuiEvidenceLocker extends Gui {
    private final FLPlayer flp;
    private final List<List<ItemStack>> inventories;
    private final List<String> punishments;
    private int currentPunishment;

    private void init() {
        NBTTagCompound locker = FarLands.getDataHandler().getEvidenceLocker(flp);
        for(Punishment p : flp.getPunishments()) {
            List<ItemStack> inv0 = new ArrayList<>(54);
            NBTTagList serInv = locker.getList(p.toUniqueString(), 10);
            serInv.stream().map(base -> (NBTTagCompound)base).forEach(nbt -> inv0.add(Utils.itemStackFromNBT(nbt)));
            inventories.add(inv0);
        }
    }

    public GuiEvidenceLocker(FLPlayer flp) {
        super("Evidence Locker", flp.getPunishments().get(0).toString(), 54);
        this.flp = flp;
        this.inventories = new ArrayList<>();
        this.punishments = flp.getPunishments().stream().map(Punishment::toString).collect(Collectors.toList());
        this.currentPunishment = 0;
        init();
    }

    private void saveInventory() {
        List<ItemStack> inv0 = inventories.get(currentPunishment);
        inv0.clear();
        for(int i = 0;i < 54;++ i) {
            if(clickActions.containsKey(i))
                inv0.add(null);
            else
                inv0.add(clone(inv.getItem(i)));
        }
    }

    private void changeInventory(int move) {
        saveInventory();
        currentPunishment += move;
        newInventory(54, punishments.get(currentPunishment));
    }

    @Override
    protected void populateInventory() {
        List<ItemStack> inv0 = inventories.get(currentPunishment);
        for(int i = 0;i < inv0.size();++ i)
            inv.setItem(i, clone(inv0.get(i)));

        if(currentPunishment < punishments.size() - 1)
            addActionItem(53, Material.EMERALD_BLOCK, ChatColor.GOLD.toString() + ChatColor.BOLD + "Next", () -> changeInventory(1));
        else
            addLabel(53, Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "No Next Locker");

        if(currentPunishment > 0)
            addActionItem(45, Material.EMERALD_BLOCK, ChatColor.GOLD.toString() + ChatColor.BOLD + "Previous", () -> changeInventory(-1));
        else
            addLabel(45, Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "No Previous Locker");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onClose() {
        saveInventory();
        NBTTagCompound locker = FarLands.getDataHandler().getEvidenceLocker(flp);
        int i = 0;
        for(Punishment p : flp.getPunishments()) {
            NBTTagList serInv = new NBTTagList();
            inventories.get(i).forEach(stack -> serInv.add(Utils.itemStackToNBT(stack)));
            locker.set(p.toUniqueString(), serInv);
            ++ i;
        }
        FarLands.getDataHandler().saveEvidenceLocker(flp, locker);
        ((List<FLPlayer>)FarLands.getDataHandler().getRADH().retrieveAndStoreIfAbsent(new CopyOnWriteArrayList<>(), "evidencelocker", "editing")).remove(flp);
    }
}
