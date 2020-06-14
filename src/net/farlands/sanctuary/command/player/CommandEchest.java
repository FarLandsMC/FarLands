package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.entity.Player;

public class CommandEchest extends PlayerCommand {
    public CommandEchest() {
        super(Rank.DONOR, "Open your enderchest.", "/echest", "echest");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sender.openInventory(sender.getEnderChest());
        return true;
    }
}
