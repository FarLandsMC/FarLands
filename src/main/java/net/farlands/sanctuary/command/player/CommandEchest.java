package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.entity.Player;

public class CommandEchest extends PlayerCommand {

    public CommandEchest() {
        super(CommandData.withRank(
            "echest",
            "Open your ender chest.",
            "/echest",
            Rank.DONOR
        ).category(Category.UTILITY));
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sender.openInventory(sender.getEnderChest());
        return true;
    }
}
