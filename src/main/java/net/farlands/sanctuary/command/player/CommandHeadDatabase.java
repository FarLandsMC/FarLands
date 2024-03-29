package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandHeadDatabase extends Command {

    public CommandHeadDatabase() {
        super(
            CommandData.withRank(
                    "hdb",
                    "Open or search the Head Database GUI.",
                    "/hdb <search|i>",
                    Rank.DONOR
                )
                .category(Category.COSMETIC)
                .aliases("headdb")
        );
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Bukkit.dispatchCommand(sender, "headdatabase:hdb " + String.join(" ", args));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        List<String> values = Collections.emptyList();
        switch (args.length) {
            case 1 -> {
                values = new ArrayList<>(Arrays.asList("search", "info"));
                if (sender.isOp()) {
                    values.addAll(Arrays.asList("base64", "give", "open", "random", "reload"));
                }
            }
            case 2 -> {
                values = switch (args[1]) {
                    case "random", "open" -> getOnlinePlayers(args[1], sender);
                    case "search" -> Collections.singletonList("recent");
                    default -> values;
                };
            }
        }
        return TabCompleterBase.filterStartingWith(args[args.length-1], values);
    }
}
