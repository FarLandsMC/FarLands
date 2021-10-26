package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.Logging;
import org.bukkit.command.CommandSender;

public class CommandBroadcast extends Command {
    public CommandBroadcast() {
        super(Rank.BUILDER, "Broadcast to server and discord.", "/broadcast <message>", "broadcast");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0)
            return false;
        Logging.broadcastFormatted(String.join(" ", args), true);
        return true;
    }
}
