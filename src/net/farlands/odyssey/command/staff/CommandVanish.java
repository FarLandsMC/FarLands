package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandVanish extends Command {
    public CommandVanish() {
        super(Rank.MEDIA, "Toggle on and off vanish mode.", "/vanish", "vanish");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(sender);
        boolean online = flp.isOnline();
        flp.setVanished(!flp.isVanished());
        flp.updateSessionIfOnline(false);
        if(flp.isVanished()) {
            sender.sendMessage(ChatColor.GOLD + "You are now vanished.");
            if(online) {
                FarLands.broadcast(ChatColor.YELLOW + ChatColor.BOLD.toString() + " > " +
                        ChatColor.RESET + flp.getRank().getNameColor() + flp.getUsername() + ChatColor.YELLOW + " has left.", true);
            }
        }else{
            sender.sendMessage(ChatColor.GOLD + "You are no longer vanished.");
            if(online) {
                FarLands.broadcast(ChatColor.YELLOW + ChatColor.BOLD.toString() + " > " +
                        ChatColor.RESET + flp.getRank().getNameColor() + flp.getUsername() + ChatColor.YELLOW + " has joined.", true);
            }
        }
        if(online)
            FarLands.getDiscordHandler().updateStats();
        else
            FarLands.getPDH().saveFLPlayer(flp);
        return true;
    }
}
