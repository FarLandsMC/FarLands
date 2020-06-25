package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.PlayerTrade;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

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
        for (int i = 0;i < 45 && i + page * 45 < trades.size();++ i) {
            PlayerTrade trade = trades.get(i + page * 45);
            addActionItem(i, trade.generateHead(), () -> trade.notifyOwner(user));
        }

        int totalPages = totalPages();

        if(page < totalPages - 1)
            addActionItem(53, Material.EMERALD_BLOCK, ChatColor.GOLD.toString() + ChatColor.BOLD + "Next", () -> changeInventory(1));
        else
            addLabel(53, Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "No Next Page");

        if(page > 0)
            addActionItem(45, Material.EMERALD_BLOCK, ChatColor.GOLD.toString() + ChatColor.BOLD + "Previous", () -> changeInventory(-1));
        else
            addLabel(45, Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "No Previous Page");
    }

    private static String windowName(int page) {
        return "Tradepost (Page " + (page + 1) + "/" + totalPages() + ")";
    }

    private static int totalPages() {
        return 1 + FarLands.getDataHandler().getPluginData().playerTrades.size() / 46;
    }
}
