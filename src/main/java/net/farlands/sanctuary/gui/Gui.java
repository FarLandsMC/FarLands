package net.farlands.sanctuary.gui;

import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for guis.
 */
public abstract class Gui {
    protected final String guiName;
    protected final Map<Integer, Pair<Runnable, Boolean>> clickActions;
    protected Inventory inv;
    protected Player user;
    private boolean ignoreClose;

    protected Gui(String guiName, Component displayName, int size) {
        this.guiName = guiName;
        this.clickActions = new HashMap<>();
        this.inv = Bukkit.createInventory(null, size, displayName);
        this.user = null;
        this.ignoreClose = false;
    }

    public Inventory getInventory() {
        return inv;
    }

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

    protected void setItem(int slot, Material material, Component name, Component... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false).append(name));
        meta.lore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        inv.setItem(slot, stack);
    }

    protected void setLore(int slot, Component... lore) {
        ItemStack stack = inv.getItem(slot);
        if (stack == null) return; // Don't set the lore if there's nothing there.
        ItemMeta meta = stack.getItemMeta();
        meta.lore(Arrays.asList(lore));
        stack.setItemMeta(meta);
    }

    protected void addActionItem(int slot, Material material, Component name, Runnable action, boolean rightClickOnly, Component... lore) {
        setItem(slot, material, name, lore);
        clickActions.put(slot, new Pair<>(action == null ? FLUtils.NO_ACTION : action, rightClickOnly));
    }

    protected void addActionItem(int slot, Material material, Component name, Runnable action, Component... lore) {
        addActionItem(slot, material, name, action, false, lore);
    }

    protected void addLabel(int slot, Material material, Component name, Component... lore) {
        addActionItem(slot, material, name, FLUtils.NO_ACTION, lore);
    }

    protected void addActionItem(int slot, ItemStack stack, Runnable action, boolean rightClickOnly) {
        inv.setItem(slot, stack);
        clickActions.put(slot, new Pair<>(action == null ? FLUtils.NO_ACTION : action, rightClickOnly));
    }

    protected void addActionItem(int slot, ItemStack stack, Runnable action) {
        addActionItem(slot, stack, action, false);
    }

    protected void newInventory(int size, Component displayName) {
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
        if (ignoreClose) {
            ignoreClose = false;
        } else {
            onClose();
            FarLands.getGuiHandler().removeActiveGui(this);
        }
    }

    protected void onClose() { }

    protected static ItemStack clone(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }
}
