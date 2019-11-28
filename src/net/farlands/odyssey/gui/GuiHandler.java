package net.farlands.odyssey.gui;

import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiHandler implements Listener {
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

    public void onInventoryClick(InventoryClickEvent event) {
        activeGuis.stream().filter(gui -> gui.matches(event)).forEach(gui -> gui.onItemClick(event));
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        activeGuis.stream().filter(gui -> gui.matches(event)).forEach(Gui::onInventoryClosed);
    }
}
