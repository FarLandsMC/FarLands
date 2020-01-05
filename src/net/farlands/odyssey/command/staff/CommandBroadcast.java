package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;

import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.Logging;
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
