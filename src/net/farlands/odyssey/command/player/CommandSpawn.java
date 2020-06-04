package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;

import net.farlands.odyssey.util.LocationWrapper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandSpawn extends Command {
    public CommandSpawn() {
        super(Rank.INITIATE, "Teleport to the server spawn.", "/spawn", "spawn");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }
        LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
        if (spawn == null) {
            sender.sendMessage(ChatColor.RED + "Server spawn not set! Please contact an owner, administrator, or developer and notify them of this problem.");
            return true;
        }
        if (args.length > 0 && Rank.getRank(sender).specialCompareTo(Rank.BUILDER) >= 0) { // Force another player to spawn
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
            if (flp == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            Player player = flp.getOnlinePlayer();
            if (player == null)
                flp.lastLocation = spawn;
            else
                player.teleport(spawn.asLocation());
            sender.sendMessage(ChatColor.GREEN + "Moved player to spawn.");
        } else
            ((Player) sender).teleport(spawn.asLocation());
        return true;
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/spawn [player]" : getUsage()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && Rank.getRank(sender).isStaff()
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }
}
