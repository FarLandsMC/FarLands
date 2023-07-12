package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.MessageFilter;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandRenameHome extends PlayerCommand {
    public CommandRenameHome() {
        super(Rank.INITIATE, Category.HOMES, "Change the name of one of your homes.",
                "/renamehome <oldName> <newName>", "renamehome");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {
        if (args.length < 2)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        String oldName = args[0];
        String newName = args[1];

        if (!flp.hasHome(oldName)) {
            return error(sender, "You do not have a home named {}.", oldName);
        }

        // Make sure the home name is valid
        if (args[1].isEmpty() || args[1].matches("\\s+") || MessageFilter.INSTANCE.isProfane(newName)) {
            return error(sender, "You cannot set a home with that name.");
        }

        if (newName.length() > 32) {
            return error(sender, "Home names are limited to 32 characters. Please choose a different name.");
        }

        // Make sure the player doesn't already have a home with the new name
        if (flp.hasHome(newName)) {
            return error(sender, "You have already set a home with this name.");
        }

        flp.renameHome(oldName, newName);
        success(sender, "Successfully renamed home {:aqua} to {:aqua}.", oldName, newName);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], FarLands.getDataHandler().getOfflineFLPlayer(sender)
                .homes.stream().map(Home::getName)) : Collections.emptyList();
    }
}
