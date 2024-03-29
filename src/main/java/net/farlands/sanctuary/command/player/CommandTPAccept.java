package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.TeleportRequest;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTPAccept extends PlayerCommand {
    public CommandTPAccept() {
        super(Rank.INITIATE, Category.TELEPORTING, "Accept a teleport request.", "/tpaccept [player]", true, "tpaccept",
                "tpdecline", "tpcancel", "tpacancel");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        if ("tpcancel".equalsIgnoreCase(args[0]) || "tpacancel".equalsIgnoreCase(args[0])) {
            TeleportRequest request = session.outgoingTeleportRequest;
            if (request == null) {
                return error(sender, "You have no outgoing teleport request to cancel.");
            }

            request.cancel();
            return true;
        }

        List<TeleportRequest> requests = session.incomingTeleportRequests;

        if(requests == null || requests.isEmpty()) {
            return error(sender, "You have no pending teleport requests.");
        }

        TeleportRequest request = args.length == 2 // If a certain player's request was specified
                ? requests.stream().filter(req -> req.getSender().getName().equals(args[1])).findAny().orElse(null)
                : requests.remove(0); // First in, first out

        if(request == null) {
            return error(sender, "This player has not sent you a teleport request.");
        }

        if("tpaccept".equals(args[0]))
            request.accept();
        else
            request.decline();

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        List<TeleportRequest> requests = FarLands.getDataHandler().getSession((Player)sender).incomingTeleportRequests;
        return args.length == 1 && !(requests == null || requests.isEmpty())
                ? requests.stream().map(req -> req.getSender().getName()).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
