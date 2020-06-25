package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandTrade extends PlayerCommand {
    public CommandTrade() {
        super(Rank.BARD, Category.MISCELLANEOUS, "Post a trade you are offering for the day.", "/trade <description|clear>", "trade");
    }

    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        if ("clear".equalsIgnoreCase(args[0])) {
            FarLands.getDataHandler().getPluginData().clearTrade(sender.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Your current trade for the day has been cleared.");
        } else {
            FarLands.getDataHandler().getPluginData().setTrade(sender.getUniqueId(), String.join(" ", args));
            sender.sendMessage(ChatColor.GREEN + "Your current trade for the day has been updated.");
        }

        return true;
    }
}
