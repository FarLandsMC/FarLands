package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.PlayerTrade;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Gui for /tradepost.
 */
public class GuiTradepost extends Gui {
    private int page;

    public GuiTradepost() {
        super("Tradepost", windowName(0), 54);
        this.page = 0;
    }

    private void changeInventory(int move) {
        page += move;
        newInventory(54, windowName(page));
    }

    @Override
    protected void populateInventory() {
        List<PlayerTrade> trades = new ArrayList<>(FarLands.getDataHandler().getPluginData().playerTrades.values());
        // Most clicks first
        trades.sort((a, b) -> Integer.compare(b.clicks, a.clicks));
        for (int i = 0; i < 45 && i + page * 45 < trades.size(); ++i) {
            PlayerTrade trade = trades.get(i + page * 45);
            addActionItem(i, trade.generateHead(), () -> trade.notifyOwner(user));
        }

        int totalPages = totalPages();

        if (page < totalPages - 1)
            addActionItem(53, Material.EMERALD_BLOCK, ComponentColor.gold("Next").decorate(TextDecoration.BOLD), () -> changeInventory(1));
        else
            addLabel(53, Material.REDSTONE_BLOCK, ComponentColor.red("No Next Page").decorate(TextDecoration.BOLD));

        if (page > 0)
            addActionItem(45, Material.EMERALD_BLOCK, ComponentColor.gold("Previous").decorate(TextDecoration.BOLD), () -> changeInventory(-1));
        else
            addLabel(45, Material.REDSTONE_BLOCK, ComponentColor.red("No Previous Page").decorate(TextDecoration.BOLD));
    }

    private static Component windowName(int page) {
        return Component.text(String.format("Tradepost (Page %d/%d)", page + 1, totalPages()));
    }

    private static int totalPages() {
        return 1 + FarLands.getDataHandler().getPluginData().playerTrades.size() / 46;
    }
}
