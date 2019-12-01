package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandGivePet extends PlayerCommand {
    public CommandGivePet() {
        super(Rank.INITIATE, "Transfer one of your pets to another player.", "/givepet <player|cancel>", "givepet");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0)
            return false;

        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        if (args[0].equalsIgnoreCase("cancel")) {
            session.discardTempData(this);
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

        session.putTempData(this, player, 1200L, () -> sender.sendMessage("Cancelled pet transfer mode"));
        sendFormatted(sender, "&(gold)Are you sure you want to give your pet to %0? " +
                "Click the pet you wish to transfer to confirm, or type " +
                "$(hovercmd,/givepet cancel,{&(gray)Click to Run},&(aqua)/givepet cancel) to cancel.", player.getName());

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }
}
