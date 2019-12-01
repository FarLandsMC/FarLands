package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandToPlayer extends PlayerCommand {
    public CommandToPlayer() {
        super(Rank.JR_BUILDER, "Teleport to a player.", "/to <player>", "to");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        Player player = getVanishedPlayer(args[0]);
        if(player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        sender.teleport(player.getLocation());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) : Collections.emptyList();
    }
}
