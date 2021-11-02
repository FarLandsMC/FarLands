package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.command.CommandSender;

public class CommandReboot extends Command {

    public CommandReboot() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "See when the next server reboot will occur.", "/reboot", "reboot", "restart");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(ComponentColor.gold("Next reboot in %s.", TimeInterval.formatTime(
            86459999L - ((System.currentTimeMillis() - FarLands.getFLConfig().restartTime) % 86400000L),
            false,
            TimeInterval.MINUTE
        )));
        return true;
    }
}
