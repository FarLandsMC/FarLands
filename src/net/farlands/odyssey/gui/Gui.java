package net.farlands.odyssey.gui;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;
import net.minecraft.server.v1_15_R1.NBTTagString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class Gui {
    protected final String guiName;
    protected final Map<Integer, Pair<Runnable, Boolean>> clickActions;
    protected Inventory inv;
    protected Player user;
    private boolean ignoreClose;

    protected Gui(String guiName, String displayName, int size) {
        this.guiName = guiName;
        this.clickActions = new HashMap<>();
        this.inv = Bukkit.createInventory(null, size, displayName);
        this.user = null;
        this.ignoreClose = false;
    }

    public Inventory getInventory() {
        return inv;
    }

    @SuppressWarnings("deprecation")
    public boolean matches(InventoryEvent event) {
        return inv.equals(event.getInventory());
    }

    protected abstract void populateInventory();

    public void openGui(Player player) {
        user = player;
        populateInventory();
        player.openInventory(inv);
        FarLands.getGuiHandler().registerActiveGui(this);
    }

    protected void setItem(int slot, Material material, String name, String... lore) {
        net.minecraft.server.v1_15_R1.ItemStack stack = CraftItemStack.asNMSCopy(new ItemStack(material));
        NBTTagCompound nbt = new NBTTagCompound(), display = new NBTTagCompound();
        display.setString("Name", "{\"text\":\"" + ChatColor.RESET + name + ChatColor.RESET + "\"}");
        if(lore.length > 0) {
            NBTTagList l = new NBTTagList();
            for(String lr : lore)
                l.add(NBTTagString.a("\"" + lr + "\""));
            display.set("Lore", l);
        }
        nbt.set("display", display);
        stack.setTag(nbt);
        inv.setItem(slot, CraftItemStack.asBukkitCopy(stack));
    }

    protected void setLore(int slot, String... lore) {
        net.minecraft.server.v1_15_R1.ItemStack stack = CraftItemStack.asNMSCopy(inv.getItem(slot));
        NBTTagCompound tag = stack.getTag();
        if(lore.length > 0) {
            NBTTagList l = new NBTTagList();
            for(String lr : lore)
                l.add(NBTTagString.a("\"" + lr + "\""));
            tag.getCompound("display").set("Lore", l);
        }
        stack.setTag(tag);
        inv.setItem(slot, CraftItemStack.asBukkitCopy(stack));
    }

    protected void addActionItem(int slot, Material material, String name, Runnable action, boolean rightClickOnly, String... lore) {
        setItem(slot, material, name, lore);
        clickActions.put(slot, new Pair<>(action == null ? Utils.NO_ACTION : action, rightClickOnly));
    }

    protected void addActionItem(int slot, Material material, String name, Runnable action, String... lore) {
        addActionItem(slot, material, name, action, false, lore);
    }

    protected void addLabel(int slot, Material material, String name, String... lore) {
        addActionItem(slot, material, name, Utils.NO_ACTION, lore);
    }

    protected void addActionItem(int slot, ItemStack stack, Runnable action, boolean rightClickOnly) {
        inv.setItem(slot, stack);
        clickActions.put(slot, new Pair<>(action == null ? Utils.NO_ACTION : action, rightClickOnly));
    }

    protected void addActionItem(int slot, ItemStack stack, Runnable action) {
        addActionItem(slot, stack, action, false);
    }

    protected void newInventory(int size, String displayName) {
        ignoreClose = true;
        user.closeInventory();
        inv = Bukkit.createInventory(null, size, displayName);
        user.openInventory(inv);
        refreshInventory();
    }

    protected void refreshInventory() {
        inv.clear();
        clickActions.clear();
        populateInventory();
    }

    public void onItemClick(InventoryClickEvent event) {
        Pair<Runnable, Boolean> action = clickActions.get(event.getRawSlot());
        if(action != null && (!action.getSecond() || event.isRightClick())) {
            action.getFirst().run();
            event.setCancelled(true);
        }
    }

    public void onInventoryClosed() {
        if(ignoreClose) {
            onCurrentInventoryClosed();
            ignoreClose = false;
        }else{
            onCurrentInventoryClosed();
            onClose();
            FarLands.getGuiHandler().removeActiveGui(this);
        }
    }

    protected void onCurrentInventoryClosed() { }

    protected void onClose() { }

    protected static ItemStack clone(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }
}
