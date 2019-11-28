package net.farlands.odyssey.command;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandDelay extends Command {
    public CommandDelay() {
        super(Rank.INITIATE, "Run a command after a certain delay.", "/delay <time> <command>", "delay");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length < 2)
            return false;
        long delay = TimeInterval.parseSeconds(args[0]) * 20L;
        FarLands.getScheduler().scheduleSyncDelayedTask(() -> Bukkit.dispatchCommand(sender, joinArgsBeyond(0, " ", args)), delay);
        return true;
    }
}
