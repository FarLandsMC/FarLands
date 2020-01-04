package net.farlands.odyssey.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
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
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp.numHomes() >= flp.rank.getHomes()) {
            sender.sendMessage(ChatColor.RED + "You do not have enough homes to set another.");
            return true;
        }
        Location loc = sender.getLocation();
        if (!"world".equals(loc.getWorld().getName())) {
            sender.sendMessage(ChatColor.RED + "You can only set homes in the overworld.");
            return true;
        }
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(loc);
        if (!(flp.rank.isStaff() || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.ACCESS, flags))) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set a home in this claim.");
            return true;
        }
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (args[0].equals("home"))
                sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/sethome home} by typing " +
                        "$(hovercmd,/sethome,{&(gray)Click to Run},&(dark_aqua)/sethome)!");
            name = args[0];
        }
        if (flp.hasHome(name)) {
            sendFormatted(sender, "&(red)You have already set a home with this name. " +
                    "Use $(hovercmd,/delhome %0,{&(gray)Delete home {&(white)%0}},&(dark_red)/delhome %0) to remove it or " +
                    "$(hovercmd,/movhome %0,{&(gray)Move home {&(white)%0} to current location},&(dark_red)/movhome %0) to move it.", name);
            return true;
        }
        if (args.length > 0 && (args[0].isEmpty() || args[0].matches("\\s+") || Chat.getMessageFilter().isProfane(args[0]))) {
            sender.sendMessage(ChatColor.RED + "You cannot set a home with that name.");
            return true;
        }
        if (name.length() > 32) {
            sender.sendMessage(ChatColor.RED + "Home names are limited to 32 characters. Please choose a different name.");
            return true;
        }
        flp.addHome(name, loc);
        sender.sendMessage(ChatColor.GREEN + "Set a home with name " + ChatColor.AQUA + name + ChatColor.GREEN + " at your location.");
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        if (name.equals(session.lastDeletedHomeName.getValue())) {
            sendFormatted(sender, "&(aqua)Looks like you just tried to move a home, did you know you can do this using " +
                    "$(hovercmd,/movhome %0,{&(gray)Move home {&(white)%0} to current location},&(dark_aqua)/movhome %0) ?", name);
        }
        session.lastDeletedHomeName.discard();
        return true;
    }
}
