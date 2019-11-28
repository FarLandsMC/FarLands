package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandProposeWarp extends PlayerCommand {
    private static final List<String> WARP_TYPES = Arrays.asList("shop", "showcase", "town", "public-farm", "other");

    public CommandProposeWarp() {
        super(Rank.INITIATE, "Propose a warp to be set by staff.", "/proposewarp <type> <name> <description>", "proposewarp", "warpform");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length < 3)
            return false;
        if(!WARP_TYPES.contains(args[0])) {
            sender.sendMessage(ChatColor.RED + "Invalid warp type: " + args[0]);
            return true;
        }
        if (!sender.getWorld().getName().equals("world") && !Rank.getRank(sender).isStaff()) {
            sender.sendMessage(ChatColor.RED + "You are only permitted to propose warps in the overworld");
            return true;
        }
        if(FarLands.getDataHandler().getPluginData().getWarpNames().contains(args[1])) {
            sender.sendMessage(ChatColor.RED + "A warp with that name already exists, please choose another name.");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("New **").append(args[0]).append("** warp proposal from `").append(sender.getName()).append("`\n");
        sb.append("Name: `").append(args[1]).append("`\n");
        Location l = sender.getLocation();
        sb.append("Location: `/tl ").append(Math.floor(l.getX()) + 0.5).append(' ').append((int)l.getY()).append(' ')
                .append(Math.floor(l.getZ()) + 0.5).append(' ').append((int)l.getYaw()).append(' ').append((int)l.getPitch())
                .append(' ').append(l.getWorld().getName()).append("`\n");
        sb.append("Description:\n```").append(joinArgsBeyond(1, " ", args)).append("```");
        FarLands.getDiscordHandler().sendMessageRaw("warpproposals", sb.toString());
        sender.sendMessage(ChatColor.GOLD + "Warp proposal sent.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? WARP_TYPES.stream().filter(type -> type.startsWith(args.length == 0 ? "" : args[0]))
                        .collect(Collectors.toList())
                : Collections.emptyList();
    }
}
