package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandDelHome extends PlayerCommand {
    public CommandDelHome() {
        super(Rank.INITIATE, "Delete a home you have already set.", "/delhome [name=home]", "delhome");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Whether or not we're deleting someone else's home
        boolean deleteUnownedHome = Rank.getRank(sender).isStaff() && args.length > 1;

        OfflineFLPlayer flp = deleteUnownedHome ? FarLands.getDataHandler().getOfflineFLPlayer(args[1])
                : FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (args[0].equals("home")) {
                sendFormatted(sender, "&(aqua)You can simplify {&(dark_aqua)/delhome home} by typing " +
                        "$(hovercmd,/delhome,{&(gray)Click to Run},&(dark_aqua)/delhome)!");
            }
            name = args[0];
        }

        if (!flp.hasHome(name)) {
            sendFormatted(sender, "&(red)%0 not have a home named \"%1\"",
                    deleteUnownedHome ? flp.username + " does" : "You do", name);
            return false;
        }

        flp.removeHome(name);

        // Keep track of their deleted home so we can notify them of /movehome if needed
        if (!deleteUnownedHome)
            FarLands.getDataHandler().getSession(sender).lastDeletedHomeName.setValue(name, 300L, null);

        sender.sendMessage(ChatColor.GREEN + "Removed home " + ChatColor.AQUA + name);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], FarLands.getDataHandler().getOfflineFLPlayer(sender).homes.stream().map(Home::getName))
                : (Rank.getRank(sender).isStaff() ? getOnlinePlayers(args[1], sender) : Collections.emptyList()); // For staff
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/delhome <name> [player]" : getUsage()));
    }
}
