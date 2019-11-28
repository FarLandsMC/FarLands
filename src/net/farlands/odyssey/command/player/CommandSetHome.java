package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CommandSetHome extends PlayerCommand {
    public CommandSetHome() {
        super(Rank.INITIATE, "Set a home where you are standing. Access your homes later with /home.", "/sethome [name=\"home\"]", "sethome");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayer flp = FarLands.getDataHandler().getPDH().getFLPlayer(sender);
        if(flp.numHomes() >= flp.getRank().getHomes()) {
            sender.sendMessage(ChatColor.RED + "You do not have enough homes to set another.");
            return true;
        }
        String name = args.length == 0 ? "home" : args[0];
        if(flp.hasHome(name)) {
            sender.sendMessage(ChatColor.RED + "You have already set a home with this name. Use /delhome to remove it so you may reset it.");
            return true;
        }
        if(args.length > 0 && (args[0].isEmpty() || args[0].matches("\\s+") || Chat.getMessageFilter().isProfane(args[0]))) {
            sender.sendMessage(ChatColor.RED + "You cannot set a home with that name.");
            return true;
        }
        Location loc = sender.getLocation();
        if(!"world".equals(loc.getWorld().getName())) {
            sender.sendMessage(ChatColor.RED + "You can only set homes in the overworld.");
            return true;
        }
        if(name.length() > 32) {
            sender.sendMessage(ChatColor.RED + "Home names are limited to 32 characters. Please choose a different name.");
            return true;
        }
        flp.addHome(name, loc);
        sender.sendMessage(ChatColor.GREEN + "Set a home with name " + ChatColor.AQUA + name + ChatColor.GREEN + " at your location.");
        if (name.equals(FarLands.getDataHandler().getRADH().retrieveString("delhome", sender.getName())))
            sender.sendMessage(ChatColor.GREEN + "Looks like you just tried to move a home," +
                    "did you know you can do this using /movhome <" + ChatColor.AQUA + name + ChatColor.GREEN + "> ?");
        FarLands.getDataHandler().getRADH().delete("delhome", sender.getName());
        return true;
    }
}
