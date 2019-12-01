package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandStaffChat extends Command {
    public CommandStaffChat() {
        super(Rank.INITIATE, "Send a message to only online staff.", "/c <message>", true, "c", "ac", "ctoggle");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(!Rank.getRank(sender).isStaff()) { // Try to make it look like an invalid command
            sender.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }

        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(sender);

        if("ctoggle".equals(args[0]) && flp != null) {
            boolean toggle = FarLands.getDataHandler().getRADH().flipBoolean(true, "staffChatToggle",
                    flp.getUuid().toString());
            sender.sendMessage(ChatColor.GOLD + "Staff chat toggled " + (toggle ? "on." : "off."));
        }else {
            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You must be online to toggle staff chat.");
                    return true;
                }
                String msg = FarLands.getDataHandler().getRADH().flipBoolean("staffchat", ((Player) sender).getUniqueId().toString())
                        ? "Staff chat toggled on." : "Staff chat toggled off.";
                sender.sendMessage(ChatColor.GREEN + msg);
                return true;
            }

            String message = joinArgsBeyond(0, " ", args);
            FarLands.broadcastStaff(ChatColor.RED + "[SC] " + sender.getName() + ": " + message);
            FarLands.getDiscordHandler().sendMessage("staffcommands", sender.getName() + ": " + message);
        }

        return true;
    }
}
