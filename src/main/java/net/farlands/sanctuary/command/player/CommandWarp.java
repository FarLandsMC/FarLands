package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandWarp extends PlayerCommand {
    public CommandWarp() {
        super(Rank.INITIATE, Category.TELEPORTING, "Teleport to a server warp.", "/warp <warp>", "warp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0) {
            // Do this so that staff don't get the double command log of `/warp` and `/warps`
            FarLands.getCommandHandler().getCommand(CommandWarps.class).execute(sender, new String[0]);
            return true;
        }
        Location warp = FarLands.getDataHandler().getPluginData().getWarp(args[0]);
        if (warp == null) {
            sender.sendMessage(ComponentColor.red("Warp not found. Did you spell it correctly?"));
            return true;
        }
        FLUtils.tpPlayer(sender, warp);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? FarLands.getDataHandler().getPluginData().getWarpNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
                        .collect(Collectors.toList())
                : Collections.emptyList();
    }
}
