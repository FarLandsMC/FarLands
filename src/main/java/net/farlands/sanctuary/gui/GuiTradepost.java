package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.PlayerTrade;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gui for `/tradepost`
 */
public class GuiTradepost extends Gui {

    private int page;

    public GuiTradepost() {
        super(windowName(0), 54);
        this.page = 0;
    }

    private void changeInventory(int move) {
        this.page += move;
        newInventory(54, windowName(this.page));
    }

    @Override
    protected void populateInventory() {
        List<PlayerTrade> trades = new ArrayList<>(FarLands.getDataHandler().getPluginData().playerTrades.values());
        trades.sort(Comparator.comparingInt(tp -> tp.clicks)); // Sort by clicks
        for (int i = 0; i < 45 && i + this.page * 45 < trades.size(); ++i) {
            PlayerTrade trade = trades.get(i + this.page * 45);
            addActionItem(i, trade.generateHead(), () -> trade.notifyOwner(this.user));
        }

        int totalPages = totalPages();

        if (this.page < totalPages - 1) { // Next page button
            addActionItem(53, Material.EMERALD_BLOCK, ComponentColor.gold("Next").decorate(TextDecoration.BOLD), () -> changeInventory(1));
        } else {
            addLabel(53, Material.REDSTONE_BLOCK, ComponentColor.red("No Next Page").decorate(TextDecoration.BOLD));
        }

        if (this.page > 0) { // Previous page button
            addActionItem(45, Material.EMERALD_BLOCK, ComponentColor.gold("Previous").decorate(TextDecoration.BOLD), () -> changeInventory(-1));
        } else {
            addLabel(45, Material.REDSTONE_BLOCK, ComponentColor.red("No Previous Page").decorate(TextDecoration.BOLD));
        }
    }

    /**
     * Generate a component for the given page index
     */
    private static Component windowName(@Range(from = 0, to = Integer.MAX_VALUE - 1) int page) {
        return ComponentUtils.format("Tradepost (Page {}/{})", page + 1, totalPages());
    }

    private static int totalPages() {
        return 1 + FarLands.getDataHandler().getPluginData().playerTrades.size() / 46;
    }
}
