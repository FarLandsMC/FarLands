package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandGameMode extends PlayerCommand {
    public CommandGameMode() {
        super(Rank.JR_BUILDER, "Switch game modes.", "/gmc|/gms|/gm3|/spec [player]", true, "gmc", "gms", "gm3", "spec");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if(!("spec".equals(args[0]) || "gm3".equals(args[0])) && Rank.BUILDER.specialCompareTo(Rank.getRank(player)) > 0) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        switch (args[0]) {
            case "gmc":
                player.setGameMode(GameMode.CREATIVE);
                break;
            case "gms":
                player.setFallDistance(0);
                player.setGameMode(GameMode.SURVIVAL);
                break;
            case "gm3":
                player.setGameMode(GameMode.SPECTATOR);
                break;
            default:
                if (args.length > 1) {
                    Player p = getPlayer(args[1]);
                    if(p == null) {
                        player.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    player.setGameMode(GameMode.SPECTATOR);
                    FarLands.getPDH().getFLPlayer(player).updateOnline(player, false);
                    player.teleport(p.getLocation());
                    return true;
                }
                player.setFallDistance(0);
                player.setGameMode(GameMode.SPECTATOR.equals(player.getGameMode()) ? GameMode.SURVIVAL : GameMode.SPECTATOR);
                FarLands.getPDH().getFLPlayer(player).updateOnline(player, false);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0]) : Collections.emptyList();
    }
}
