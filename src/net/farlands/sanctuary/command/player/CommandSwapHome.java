package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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

        if (!flp.hasHome(args[0])) {
            sender.sendMessage(ChatColor.RED + "You do not have a home named " + args[0]);
            return true;
        }

        if (!flp.hasHome(args[1])) {
            sender.sendMessage(ChatColor.RED + "You do not have a home named " + args[1]);
            return true;
        }

        // I'm sure kish will come back to optimize this travesty at some point
        Location firstHome = flp.getHome(args[0]);
        Location secondHome = flp.getHome(args[1]);
        flp.moveHome(args[0], secondHome);
        flp.moveHome(args[1], firstHome);

        sender.sendMessage(ChatColor.GREEN + "Successfully swapped home locations.");
        return true;
    }
}
