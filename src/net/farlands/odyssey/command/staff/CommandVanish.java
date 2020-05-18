package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandVanish extends Command {
    public CommandVanish() {
        super(Rank.MEDIA, "Toggle on and off vanish mode.", "/vanish", "vanish");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        boolean online = flp.isOnline();
        flp.vanished = !flp.vanished;
        flp.updateSessionIfOnline(false);
        if (flp.vanished) {
            sender.sendMessage(ChatColor.GOLD + "You are now vanished.");
            if (online) {
                Logging.broadcast(ChatColor.YELLOW + ChatColor.BOLD.toString() + " > " +
                        ChatColor.RESET + flp.rank.getNameColor() + flp.username + ChatColor.YELLOW + " has left.", true);
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + "You are no longer vanished.");
            if (online) {
                Logging.broadcast(ChatColor.YELLOW + ChatColor.BOLD.toString() + " > " +
                        ChatColor.RESET + flp.rank.getNameColor() + flp.username + ChatColor.YELLOW + " has joined.", true);
            }
        }
        if (online)
            FarLands.getDiscordHandler().updateStats();
        return true;
    }
}
