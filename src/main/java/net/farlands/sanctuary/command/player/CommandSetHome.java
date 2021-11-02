package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Chat;

import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CommandSetHome extends PlayerCommand {
    public CommandSetHome() {
        super(Rank.INITIATE, Category.HOMES, "Set a home where you are standing. Access your homes later with /home.",
                "/sethome [homeName]", "sethome");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Check home count
        if (!flp.canAddHome()) {
            sender.sendMessage(ComponentColor.red("You have reached the maximum number of homes you can have at your current rank."));
            return true;
        }

        // Check to make sure they're not setting a home in an off-limits dimension
        Location location = sender.getLocation();
        if (!(
                "world".equals(location.getWorld().getName()) ||
                "world_nether".equals(location.getWorld().getName()) ||
                "farlands".equals(location.getWorld().getName())
        )) {
            sender.sendMessage(ComponentColor.red("You can only set homes in the overworld and nether."));
            return true;
        }

        // Check for claims
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(location);
        if (!(flags == null || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.ACCESS, flags))) {
            sender.sendMessage(ComponentColor.red("You do not have permission to set a home in this claim."));
            return true;
        }

        // Determine the name to use
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            // Notify the sender of the default value
            if (args[0].equals("home")) {
                sender.sendMessage(ComponentColor.aqua("You can simplify ")
                                       .append(ComponentColor.darkAqua("/sethome home"))
                                       .append(ComponentColor.aqua(" by typing "))
                                       .append(ComponentUtils.command("/sethome", NamedTextColor.DARK_AQUA))
                                       .append(ComponentColor.aqua("!"))
                );
            }

            name = args[0];
        }

        // If they already have that home cancel the creation and notify them of delhome and movehome
        if (flp.hasHome(name)) {
            sender.sendMessage(
                ComponentColor.red("You have already set a home with this name. Use ")
                    .append(ComponentUtils.command("/delhome " + name, NamedTextColor.DARK_RED))
                    .append(ComponentColor.red(" to remove it or "))
                    .append(ComponentUtils.command("/movhome " + name))
                    .append(ComponentColor.red(" to move it."))
            );
            return true;
        }

        // Make sure the home name is valid
        if (args.length > 0 && (args[0].isEmpty() || args[0].matches("\\s+") || Chat.getMessageFilter().isProfane(args[0]) || args[0].matches("[&$%]"))) {
            sender.sendMessage(ComponentColor.red("You cannot set a home with that name."));
            return true;
        }

        if (name.length() > 32) {
            sender.sendMessage(ComponentColor.red("Home names are limited to 32 characters. Please choose a different name."));
            return true;
        }

        // Add the home
        flp.addHome(name, location);
        sender.sendMessage(
            ComponentColor.green("Set a home with the name ")
                .append(ComponentColor.aqua(name))
                .append(ComponentColor.green(" at your current location."))
        );

        // The sender recently did /delhome on a home with the same name, so notify them of /movehome
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        if (name.equals(session.lastDeletedHomeName.getValue())) {
            sender.sendMessage(
                ComponentColor.aqua("It looks like you just tried to move a home, did you know that you can do this using ")
                    .append(ComponentUtils.command("/movhome " + name, NamedTextColor.DARK_AQUA))
                    .append(ComponentColor.aqua("?"))
            );
        }
        session.lastDeletedHomeName.discard();

        return true;
    }
}
