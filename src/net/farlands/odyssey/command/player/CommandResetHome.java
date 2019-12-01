package net.farlands.odyssey.command.player;


import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandResetHome extends PlayerCommand {
    public CommandResetHome() {
        super(Rank.INITIATE, "Move a home where you are standing.", "/resethome [name=\"home\"]", "resethome", "movehome", "movhome");
    }
    
    @Override
    public boolean execute(Player sender, String[] args) {
        boolean flag = Rank.getRank(sender).isStaff() && args.length > 1; // Is the sender a staff member and are they moving someone else's home
        OfflineFLPlayer flp = flag ? getFLPlayer(args[1]) : FarLands.getDataHandler().getPDH().getFLPlayer(sender);
        Location loc = sender.getLocation();
        if(!"world".equals(loc.getWorld().getName())) {
            sender.sendMessage(ChatColor.RED + "You can only move homes to the overworld. Reset cancelled");
            return true;
        }
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(loc);
        if (!(flp.getRank().isStaff() || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.ACCESS, flags))) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to move a home into this claim.");
            return true;
        }
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (args[0].equals("home"))
                sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/movhome home} by typing " +
                        "$(hovercmd,/movhome,{&(gray)Click to Run},&(dark_aqua)/movhome)!");
            name = args[0];
        }
        if(flp.hasHome(name))
            sender.sendMessage(ChatColor.GREEN + "Moved home with name " + ChatColor.AQUA + name + ChatColor.GREEN + " to your location.");
        else {
            if (flag) {
                sender.sendMessage(ChatColor.RED + args[1] + " does not have a home with this name.");
                return true;
            }
            // Create the home if it doesn't exist and the user has enough homes to set another
            if (flp.numHomes() >= flp.getRank().getHomes()) {
                sender.sendMessage(ChatColor.RED + "Home does not exist and you do not have enough homes to set another.");
                return true;
            } else {
                if(args.length > 0 && (args[0].isEmpty() || args[0].matches("\\s+") || Chat.getMessageFilter().isProfane(args[0]))) {
                    sender.sendMessage(ChatColor.RED + "Home does not exist. Unable to create home with that name.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Home does not exist, creating new home with name " + ChatColor.AQUA +
                        name + ChatColor.GREEN + " at your location.");
                flp.addHome(name, loc);
            }
        }
        flp.moveHome(name, loc);
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? FarLands.getDataHandler().getPDH().getFLPlayer(sender).getHomes().stream().map(Home::getName)
                .filter(home -> home.startsWith(args.length == 0 ? "" : args[0]))
                .collect(Collectors.toList())
                : (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args[1]) : Collections.emptyList()); // For staff
    }
    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/resethome <name> [player]" : getUsage()));
    }
}
