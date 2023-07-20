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
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CommandHome extends PlayerCommand {
    public CommandHome() {
        super(
            CommandData.simple(
                    "home",
                    "Go to a home that you have already set.",
                    "/home [homeName]"
                )
                .category(Category.HOMES)
                .aliases("hoem")
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Is the sender a staff member and are they going to someone else's home
        boolean gotoUnownedHome = Rank.getRank(sender).isStaff() && args.length > 1;

        // Get the player who owns the home
        OfflineFLPlayer flp = gotoUnownedHome ? FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1])
                : FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null) {
            return error(sender, "Player not found.");
        }

        // Get the home name
        String name;
        if (args.length <= 0)
            name = "home";
        else {
            if (!gotoUnownedHome && args[0].equals("home")) {
                sender.sendMessage(ComponentColor.aqua(
                    "You can simplify {:dark_aqua} by typing {:dark_aqua}.",
                    "/home home",
                    "/home")
                );
            }
            name = args[0];
        }

        // Make sure the home exists
        Location loc = flp.getHome(name);
        if (loc == null) {
            List<Home> matching = flp.homes.stream().filter(home -> home.getName().startsWith(name)).toList();
            if (matching.size() == 1) {
                FLUtils.tpPlayer(sender, matching.get(0).getLocation());
            } else if (matching.size() > 1) {
                sender.sendMessage(
                    ComponentColor.gold("Multiple matches found. Did you mean ")
                        .append(Component.join(
                            JoinConfiguration.separators(ComponentColor.gold(", "), ComponentColor.gold(", or ")),
                            matching.stream().map(home ->
                                                      ComponentUtils.suggestCommand(
                                                          "/home " + home.getName(),
                                                          ComponentColor.aqua(home.getName()),
                                                          ComponentColor.gray("Click to go to this home."))
                            ).toList())
                        )
                        .append(ComponentColor.gold("?"))
                );
            } else {
                error(
                    sender,
                    "{} {} not have a home named \"{}\"",
                    gotoUnownedHome ? flp : "You",
                    gotoUnownedHome ? "does" : "do",
                    name
                );
            }
            return true;
        }

        FLUtils.tpPlayer(sender, loc);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], FarLands.getDataHandler().getOfflineFLPlayer(sender).homes.stream().map(Home::getName))
                : (Rank.getRank(sender).isStaff() ? getOnlinePlayers(args[1], sender) : Collections.emptyList()); // For staff
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/home <name> [player]" : getUsage()));
    }
}
