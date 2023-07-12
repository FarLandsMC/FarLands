package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandDelHome extends PlayerCommand {
    public CommandDelHome() {
        super(
            CommandData.simple(
                    "delhome",
                    "Delete a home you have already set.",
                    "/delhome [homeName]"
                )
                .category(Category.HOMES)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Whether we're deleting someone else's home
        boolean deleteUnownedHome = Rank.getRank(sender).isStaff() && args.length > 1;

        OfflineFLPlayer flp = deleteUnownedHome ? FarLands.getDataHandler().getOfflineFLPlayer(args[1])
                : FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null) {
            sender.sendMessage(ComponentColor.red("Player not found."));
            return true;
        }

        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (args[0].equals("home")) {
                Component c = Component.text().content("You can simplify ")
                    .color(NamedTextColor.AQUA)
                    .append(ComponentColor.darkAqua("/delhome home"))
                    .append(ComponentColor.aqua(" by "))
                    .append(ComponentUtils.command("/delhome", NamedTextColor.DARK_AQUA))
                    .append(ComponentColor.aqua("!"))
                    .build();

                sender.sendMessage(c);
            }
            name = args[0];
        }

        if (!flp.hasHome(name)) {
            String user = deleteUnownedHome ? flp.username + " does" : "You do";
            sender.sendMessage(ComponentColor.red(user + " not have a home named \"" + name + "\""));
            return false;
        }

        flp.removeHome(name);

        // Keep track of their deleted home so we can notify them of /movehome if needed
        if (!deleteUnownedHome)
            FarLands.getDataHandler().getSession(sender).lastDeletedHomeName.setValue(name, 300L, null);

        sender.sendMessage(ComponentColor.green("Removed home ").append(ComponentColor.aqua(name)));
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
