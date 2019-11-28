package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.struct.TeleportRequest;
import net.farlands.odyssey.util.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandGivePet extends PlayerCommand {
    public CommandGivePet() {
        super(Rank.INITIATE, "Transfer one of your pets to another player.", "/givepet <player|cancel>", "givepet");
    }
    
    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0)
            return false;
    
        if (args[0].equalsIgnoreCase("cancel")) {
            FarLands.getDataHandler().getRADH().delete("givePet", sender.toString());
            sender.sendMessage("Cancelled pet transfer mode");
            return true;
        }
    
        Player player = getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if (sender.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You already own your own pets.");
            return true;
        }
        TextUtils.sendFormatted(sender, "&(gold)Are you sure you want to give your pet to %0? " +
                "Click the pet you wish to transfer to confirm, or type {&(aqua)/givepet cancel} to cancel.", player.getName());
        FarLands.getDataHandler().getRADH().store(player, "givePet", sender.getUniqueId().toString());
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
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0]) : Collections.emptyList();
    }
}
