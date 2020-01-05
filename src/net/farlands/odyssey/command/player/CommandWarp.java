package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.FLUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandWarp extends PlayerCommand {
    public CommandWarp() {
        super(Rank.INITIATE, "Teleport to a server warp.", "/warp <warp>", "warp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;
        Location warp = FarLands.getDataHandler().getPluginData().getWarp(args[0]);
        if (warp == null) {
            sender.sendMessage(ChatColor.RED + "Warp not found. Did you spell it correctly?");
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
