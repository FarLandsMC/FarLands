package net.farlands.odyssey.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.mechanic.Chat;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CommandSetHome extends PlayerCommand {
    public CommandSetHome() {
        super(Rank.INITIATE, "Set a home where you are standing. Access your homes later with /home.", "/sethome [name=home]", "sethome");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Check home count
        if (flp.numHomes() >= flp.rank.getHomes()) {
            TextUtils.sendFormatted(sender, "&(red)You have reached the maximum number of homes you can have at your current rank.");
            return true;
        }

        // Check to make sure they're not setting a home in an off-limits dimension
        Location location = sender.getLocation();
        if (!("world".equals(location.getWorld().getName()) || "world_nether".equals(location.getWorld().getName()))) {
            TextUtils.sendFormatted(sender, "&(red)You can only set homes in the overworld and nether.");
            return true;
        }

        // Check for claims
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(location);
        if (!(flp.rank.isStaff() || flags == null || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.ACCESS, flags))) {
            TextUtils.sendFormatted(sender, "&(red)You do not have permission to set a home in this claim.");
            return true;
        }

        // Determine the name to use
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            // Notify the sender of the default value
            if (args[0].equals("home")) {
                TextUtils.sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/sethome home} by typing " +
                        "$(hovercmd,/sethome,{&(gray)Click to Run},&(dark_aqua)/sethome)!");
            }

            name = args[0];
        }

        // If they already have that home cancel the creation and notify them of delhome and movehome
        if (flp.hasHome(name)) {
            TextUtils.sendFormatted(sender, "&(red)You have already set a home with this name. " +
                    "Use $(hovercmd,/delhome %0,{&(gray)Delete home {&(white)%0}},&(dark_red)/delhome %0) to remove it or " +
                    "$(hovercmd,/movhome %0,{&(gray)Move home {&(white)%0} to current location},&(dark_red)/movhome %0) to move it.", name);
            return true;
        }

        // Make sure the home name is valid
        if (args.length > 0 && (args[0].isEmpty() || args[0].matches("\\s+") || Chat.getMessageFilter().isProfane(args[0]))) {
            TextUtils.sendFormatted(sender, "&(red)You cannot set a home with that name.");
            return true;
        }

        if (name.length() > 32) {
            TextUtils.sendFormatted(sender, "&(red)Home names are limited to 32 characters. Please choose a different name.");
            return true;
        }

        // Add the home
        flp.addHome(name, location);
        TextUtils.sendFormatted(sender, "&(green)Set a home with name {&(aqua)%0} at your current location.", name);

        // The sender recently did /delhome on a home with the same name, so notify them of /movehome
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        if (name.equals(session.lastDeletedHomeName.getValue())) {
            TextUtils.sendFormatted(sender, "&(aqua)Looks like you just tried to move a home, did you know you can do this using " +
                    "$(hovercmd,/movhome %0,{&(gray)Move home {&(white)%0} to current location},&(dark_aqua)/movhome %0)?", name);
        }
        session.lastDeletedHomeName.discard();

        return true;
    }
}
