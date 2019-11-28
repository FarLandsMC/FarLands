package net.farlands.odyssey.command.player;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
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
