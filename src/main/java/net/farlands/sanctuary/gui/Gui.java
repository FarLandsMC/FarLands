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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for GUI implementations
 */
public abstract class Gui {

    protected final Map<Integer, Pair<Runnable, Boolean>> clickActions;

    protected Inventory inventory;
    protected Player    user;
    private   boolean   ignoreClose;

    /**
     * Create a blank GUI
     *
     * @param displayName The title of the GUI
     * @param size        The amount of slots in the GUI -- Must be a multiple of 9
     */
    protected Gui(Component displayName, @Range(from = 9, to = 54) int size) {
        this.clickActions = new HashMap<>();
        this.inventory = Bukkit.createInventory(null, size, displayName);
        this.user = null;
        this.ignoreClose = false;
    }

    /**
     * Get the inventory associated with the GUI
     */
    public Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Check if this GUI matches a provided event
     */
    public boolean matches(InventoryEvent event) {
        return this.inventory.equals(event.getInventory());
    }

    /**
     * Populate the inventory with items -- Called when the inventory is opened for a player
     */
    protected abstract void populateInventory();

    /**
     * Open the GUI for the provided player
     */
    public void openGui(Player player) {
        this.user = player;
        populateInventory();
        player.openInventory(this.inventory);
        FarLands.getGuiHandler().registerActiveGui(this);
    }

    /**
     * Set an item at a slot
     *
     * @param slot     The slot to set, must be between 0 and size - 1
     * @param material Material for the itemstack
     * @param name     Name for the itemstack
     * @param lore     Lore for the itemstack
     */
    protected void setItem(@Range(from = 0, to = 53) int slot, Material material, Component name, Component... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false).append(name));
        meta.lore(List.of(lore));
        stack.setItemMeta(meta);
        this.inventory.setItem(slot, stack);
    }

    /**
     * Set the lore of an item in a specific slot
     *
     * @param slot Slot for the item -- must be between 0 and size - 1
     * @param lore The new lore
     */
    protected void setLore(@Range(from = 0, to = 53) int slot, Component... lore) {
        ItemStack stack = this.inventory.getItem(slot);
        if (stack == null) return; // Don't set the lore if there's nothing there.
        ItemMeta meta = stack.getItemMeta();
        meta.lore(List.of(lore));
        stack.setItemMeta(meta);
    }

    /**
     * Add an item that runs an event when clicked
     *
     * @param slot           Slot to add the item -- must be between 0 and size - 1
     * @param material       Material for the itemstack
     * @param name           Name for the itemstack
     * @param action         The action to run when the item is clicked
     * @param rightClickOnly Whether the event should only run when the item is right clicked
     * @param lore           Lore for the itemstack
     */
    protected void addActionItem(@Range(from = 0, to = 53) int slot, Material material, Component name, Runnable action, boolean rightClickOnly, Component... lore) {
        setItem(slot, material, name, lore);
        this.clickActions.put(slot, new Pair<>(action == null ? FLUtils.NO_ACTION : action, rightClickOnly));
    }

    /**
     * Add an item that runs an event when clicked
     *
     * @param slot     Slot to add the item -- must be between 0 and size - 1
     * @param material Material for the itemstack
     * @param name     Name for the itemstack
     * @param action   The action to run when the item is clicked
     * @param lore     Lore for the itemstack
     */
    protected void addActionItem(@Range(from = 0, to = 53) int slot, Material material, Component name, Runnable action, Component... lore) {
        addActionItem(slot, material, name, action, false, lore);
    }

    /**
     * Add an item that does nothing when clicked
     *
     * @param slot     Slot to add the item -- must be between 0 and size - 1
     * @param material Material for the itemstack
     * @param name     Name for the itemstack
     * @param lore     Lore for the itemstack
     */
    protected void addLabel(@Range(from = 0, to = 53) int slot, Material material, Component name, Component... lore) {
        addActionItem(slot, material, name, FLUtils.NO_ACTION, lore);
    }

    /**
     * Add item that runs an event when clicked
     *
     * @param slot           Slot to add the item -- must be between 0 and size - 1
     * @param stack          The ItemStack to add
     * @param action         The action to run when the item is clicked
     * @param rightClickOnly Whether the event should only run when the item is right clicked
     */
    protected void addActionItem(@Range(from = 0, to = 53) int slot, ItemStack stack, Runnable action, boolean rightClickOnly) {
        this.inventory.setItem(slot, stack);
        this.clickActions.put(slot, new Pair<>(action == null ? FLUtils.NO_ACTION : action, rightClickOnly));
    }

    /**
     * Add item that runs an event when clicked
     *
     * @param slot   Slot to add the item -- must be between 0 and size - 1
     * @param stack  The ItemStack to add
     * @param action The action to run when the item is clicked
     */
    protected void addActionItem(@Range(from = 0, to = 53) int slot, ItemStack stack, Runnable action) {
        addActionItem(slot, stack, action, false);
    }

    /**
     * Create a new inventory for this GUI
     *
     * @param size        The new inventory size -- Must be a multiple of 9
     * @param displayName The new inventory display name
     */
    protected void newInventory(@Range(from = 9, to = 54) int size, Component displayName) {
        this.ignoreClose = true;
        this.user.closeInventory();
        this.inventory = Bukkit.createInventory(null, size, displayName);
        this.user.openInventory(this.inventory);
        refreshInventory();
    }

    /**
     * Refresh all items in the GUI
     * <br>
     * Clears the items and then adds them back using {@link #populateInventory()}
     */
    protected void refreshInventory() {
        this.inventory.clear();
        this.clickActions.clear();
        populateInventory();
    }

    /**
     * Event to handle clicks in the GUI
     * <br>
     * Called from {@link GuiHandler}
     */
    public void onItemClick(InventoryClickEvent event) {
        Pair<Runnable, Boolean> action = this.clickActions.get(event.getRawSlot());
        if (action != null && (!action.getSecond() || event.isRightClick())) {
            action.getFirst().run();
            event.setCancelled(true);
        }
    }

    /**
     * Runs when the GUI is closed
     * <br>
     * Called from {@link GuiHandler}
     */
    public void onInventoryClosed() {
        if (this.ignoreClose) {
            this.ignoreClose = false;
        } else {
            onClose();
            FarLands.getGuiHandler().removeActiveGui(this);
        }
    }

    /**
     * Method run when the GUI is closed by the player
     */
    protected void onClose() {
    }

    /**
     * Clone an item stack -- null safe
     */
    protected static @Nullable ItemStack clone(@Nullable ItemStack stack) {
        return stack == null ? null : stack.clone();
    }
}
