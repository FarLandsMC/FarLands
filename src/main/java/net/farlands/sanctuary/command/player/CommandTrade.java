package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandTrade extends PlayerCommand {
    public CommandTrade() {
        super(Rank.BARD, Category.MISCELLANEOUS, "Post a trade you are offering for the day.", "/trade <description|clear>", "trade");
    }

    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        if ("clear".equalsIgnoreCase(args[0])) {
            FarLands.getDataHandler().getPluginData().clearTrade(sender.getUniqueId());
            sender.sendMessage(ComponentColor.green("Your current trade for the day has been cleared."));
        } else {
            FarLands.getDataHandler().getPluginData().setTrade(sender.getUniqueId(),
                    Chat.applyColorCodes(Rank.getRank(sender), String.join(" ", args)));
            sender.sendMessage(ComponentColor.green("Your current trade for the day has been updated."));
        }

        return true;
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && "clear".startsWith(args.length == 0 ? "" : args[0])
                ? Collections.singletonList("clear") : Collections.emptyList();
    }
}
