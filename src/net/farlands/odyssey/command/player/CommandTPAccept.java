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
import java.util.stream.Collectors;

public class CommandTPAccept extends PlayerCommand {
    public CommandTPAccept() {
        super(Rank.INITIATE, "Accept a teleport request.", "/tpaccept [player]", true, "tpaccept", "tpdecline");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Player sender, String[] args) {
        List<TeleportRequest> requests = FarLands.getDataHandler().getSession(sender).teleportRequests;
        if(requests == null || requests.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
            return true;
        }
        TeleportRequest request = args.length == 2 // If a certain player's request was specified
                ? requests.stream().filter(req -> req.getSender().getName().equals(args[1])).findAny().orElse(null)
                : requests.remove(0); // First in, first out
        if(request == null) {
            sender.sendMessage(ChatColor.RED + "This player has not sent you a teleport request.");
            return true;
        }
        if("tpaccept".equals(args[0]))
            request.accept();
        else
            request.decline();
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        List<TeleportRequest> requests = FarLands.getDataHandler().getSession((Player)sender).teleportRequests;
        return args.length == 1 && !(requests == null || requests.isEmpty())
                ? requests.stream().map(req -> req.getSender().getName()).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
