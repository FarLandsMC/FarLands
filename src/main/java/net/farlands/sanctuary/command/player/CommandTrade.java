package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandTrade extends PlayerCommand {

    public CommandTrade() {
        super(
            CommandData.withRank(
                "trade",
                "Post a trade you are offering for the day.",
                "/trade <description|clear>",
                Rank.BARD
            ).category(Category.MISCELLANEOUS)
        );
    }

    public boolean execute(Player sender, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if ("clear".equalsIgnoreCase(args[0])) {
            FarLands.getDataHandler().getPluginData().clearTrade(sender.getUniqueId());
            success(sender, "Your current trade for the day has been cleared.");
        } else {
            FarLands.getDataHandler()
                .getPluginData()
                .setTrade(
                    sender.getUniqueId(),
                    ComponentUtils.parse(String.join(" ", args), FarLands.getDataHandler().getOfflineFLPlayer(sender))
                );
            success(sender, "Your current trade for the day has been updated.");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && "clear".startsWith(args.length == 0 ? "" : args[0])
            ? Collections.singletonList("clear")
            : Collections.emptyList();
    }
}
