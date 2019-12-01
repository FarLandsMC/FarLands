package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandGivePet extends PlayerCommand {
    private final Map<UUID, Player> transfers;

    public CommandGivePet() {
        super(Rank.INITIATE, "Transfer one of your pets to another player.", "/givepet <player|cancel>", "givepet");
        this.transfers = new HashMap<>();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0)
            return false;

        if (args[0].equalsIgnoreCase("cancel")) {
            transfers.remove(sender.getUniqueId());
            sender.sendMessage("Cancelled pet transfer mode");
            return true;
        }

        Player player = Rank.getRank(sender).isStaff() ? getVanishedPlayer(args[0]) : getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if (sender.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You already own your own pets.");
            return true;
        }
        sendFormatted(sender, "&(gold)Are you sure you want to give your pet to %0? " +
                "Click the pet you wish to transfer to confirm, or type " +
                "$(hovercmd,/givepet cancel,{&(gray)Click to Run},&(aqua)/givepet cancel) to cancel.", player.getName());
        transfers.put(sender.getUniqueId(), player);
        FarLands.getScheduler().scheduleSyncDelayedTask(() -> {
            
        })
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            if (FarLands.getDataHandler().getRADH().retrieve("givePet", sender.getUniqueId().toString()) != null) {
                sender.sendMessage("Cancelled pet transfer mode");
                FarLands.getDataHandler().getRADH().delete("givePet", sender.getUniqueId().toString());
            }
        }, 1200);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }
}
