package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.mechanic.Mechanic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles active guis.
 */
public class GuiHandler extends Mechanic {
    private final List<Gui> activeGuis;

    public GuiHandler() {
        this.activeGuis = new CopyOnWriteArrayList<>();
    }

    public void registerActiveGui(Gui gui) {
        activeGuis.add(gui);
    }

    public void removeActiveGui(Gui gui) {
        activeGuis.remove(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        activeGuis.stream().filter(gui -> gui.matches(event)).forEach(gui -> gui.onItemClick(event));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        activeGuis.stream().filter(gui -> gui.matches(event)).forEach(Gui::onInventoryClosed);
    }
}