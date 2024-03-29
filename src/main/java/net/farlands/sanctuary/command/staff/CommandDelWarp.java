package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.PluginData;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandDelWarp extends PlayerCommand {
    public CommandDelWarp() {
        super(Rank.BUILDER, "Delete a public warp.", "/delwarp <name>", "delwarp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        PluginData pd = FarLands.getDataHandler().getPluginData();
        if(!pd.getWarpNames().contains(args[0])) {
            return error(sender, "Warp not found.");
        }
        pd.removeWarp(args[0]);
        return success(sender, "Warp removed.");
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? FarLands.getDataHandler().getPluginData().getWarpNames().stream()
                .filter(name -> name.toLowerCase().startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
                .collect(Collectors.toList())
                : Collections.emptyList();
    }
}
