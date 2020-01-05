package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.util.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandStaffChat extends Command {
    public CommandStaffChat() {
        super(Rank.INITIATE, "Send a message to only online staff.", "/c <message>", true, "c", "ac", "ctoggle");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!Rank.getRank(sender).isStaff()) { // Try to make it look like an invalid command
            sender.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }

        // Toggling
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must be online to toggle staff chat.");
                return true;
            }

            FLPlayerSession session = FarLands.getDataHandler().getSession((Player) sender);
            if ("ctoggle".equals(args[0])) {
                sender.sendMessage(ChatColor.GREEN + "Staff chat toggled " +
                        ((session.showStaffChat = !session.showStaffChat) ? "on." : "off."));
            }else{
                sender.sendMessage(ChatColor.GREEN + "Staff chat auto-messaging toggled " +
                        ((session.autoSendStaffChat = !session.autoSendStaffChat) ? "on." : "off."));
            }
        }
        // Sending a message
        else {
            String message = joinArgsBeyond(0, " ", args);
            Logging.broadcastStaff(ChatColor.RED + "[SC] " + sender.getName() + ": " + message);
            FarLands.getDiscordHandler().sendMessage("staffcommands", sender.getName() + ": " + message);
        }

        return true;
    }
}
