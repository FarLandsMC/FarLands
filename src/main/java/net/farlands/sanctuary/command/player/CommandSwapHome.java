package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandSwapHome extends PlayerCommand {
    public CommandSwapHome() {
        super(Rank.INITIATE, Category.HOMES, "Swap the locations of two of your homes.", "/swaphome <firstHome> <secondHome>",
                "swaphome", "swaphomes");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length < 2)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        Location firstHome = flp.getHome(args[0]);
        if (firstHome == null) {
            sender.sendMessage(ComponentColor.red("You do not have a home named " + args[0]));
            return true;
        }

        Location secondHome = flp.getHome(args[1]);
        if (secondHome == null) {
            sender.sendMessage(ComponentColor.red("You do not have a home named " + args[1]));
            return true;
        }

        // I'm sure kish will come back to optimize this travesty at some point
        flp.moveHome(args[0], secondHome);
        flp.moveHome(args[1], firstHome);

        sender.sendMessage(ComponentColor.green("Successfully swapped home locations."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            // All home names
            case 1:
                return TabCompleterBase.filterStartingWith(
                        args[0],
                        FarLands.getDataHandler().getOfflineFLPlayer(sender).homes.stream().map(Home::getName)
                );

            // All home names excluding the first home provided
            case 2:
                return TabCompleterBase.filterStartingWith(
                        args[1],
                        FarLands.getDataHandler().getOfflineFLPlayer(sender).homes.stream()
                                .map(Home::getName)
                                .filter(homeName -> !args[0].equals(homeName))
                );

            default:
                return Collections.emptyList();
        }
    }
}
