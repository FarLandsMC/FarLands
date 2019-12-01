package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.TeleportRequest;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandTPA extends PlayerCommand {
    public CommandTPA() {
        super(Rank.INITIATE, "Request to teleport to another player.", "/tpa <player>", true, "tpa", "tpahere");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 1)
            return false;
        Player player = Rank.getRank(sender).isStaff() ? getVanishedPlayer(args[1]) : getPlayer(args[1]);
        if(player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if(sender.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You cannot teleport to yourself.");
            return true;
        }
        if(FarLands.getDataHandler().getRADH().retrieveBoolean("afkCmd", player.getUniqueId().toString()))
            sender.sendMessage(ChatColor.RED + "This player is AFK, so they may not receive your request.");
        if(FarLands.getPDH().getFLPlayer(player).isIgnoring(sender.getUniqueId()))
            return true;
        // Everything else is handled here
        TeleportRequest.newRequest("tpa".equals(args[0])
                ? TeleportRequest.TeleportType.SENDER_TO_RECIPIENT
                : TeleportRequest.TeleportType.RECIPIENT_TO_SENDER,
                sender, player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }
}
