package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.mechanic.Mechanic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handle all GUI events
 */
public class GuiHandler extends Mechanic {
    private final List<Gui> activeGuis;

    public GuiHandler() {
        this.activeGuis = new CopyOnWriteArrayList<>();
    }

    /**
     * Register a new GUI that has been opened
     */
    public void registerActiveGui(Gui gui) {
        this.activeGuis.add(gui);
    }

    /**
     * Remove an opened GUI
     */
    public void removeActiveGui(Gui gui) {
        this.activeGuis.remove(gui);
    }

    /**
     * Handle item clicks in GUIs -- event is sent to the GUI in which the item has been clicked
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        this.activeGuis.stream().filter(gui -> gui.matches(event)).forEach(gui -> gui.onItemClick(event));
    }

    /**
     * Handle inventory close -- event is sent to the GUI that has been closed (or attempted to close)
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        this.activeGuis.stream().filter(gui -> gui.matches(event)).forEach(Gui::onInventoryClosed);
    }
}
