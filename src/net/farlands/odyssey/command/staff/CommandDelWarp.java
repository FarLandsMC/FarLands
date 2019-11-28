package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.PluginData;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "Warp not found.");
            return true;
        }
        pd.removeWarp(args[0]);
        sender.sendMessage(ChatColor.GREEN + "Warp removed.");
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
