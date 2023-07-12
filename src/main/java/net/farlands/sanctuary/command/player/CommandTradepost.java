package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.gui.GuiTradepost;
import org.bukkit.entity.Player;

public class CommandTradepost extends PlayerCommand {
    public CommandTradepost() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "View trades that players are offering.", "/tradepost", "tradepost");
    }

    public boolean execute(Player sender, String[] args) {
        info(sender, "Click on a player's head to notify them that you would like to trade with them.");
        new GuiTradepost().openGui(sender);
        return true;
    }
}
