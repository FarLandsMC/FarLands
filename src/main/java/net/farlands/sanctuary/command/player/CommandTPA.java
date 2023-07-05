package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.struct.TeleportRequest;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandTPA extends PlayerCommand {
    public CommandTPA() {
        super(
            CommandData.simple(
                    "tpa",
                    "Request to teleport to another player or for another player to teleport to you",
                    "/tpa|tpahere <player>"
                )
                .aliases(true, "tpahere")
                .category(Category.TELEPORTING)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 1)
            return false;

        if (FarLands.getDataHandler().getSession(sender).outgoingTeleportRequest != null) {
            sender.sendMessage(ComponentColor.red("You already have an outgoing teleport request."));
            return true;
        }

        Player recipient = getPlayer(args[1], sender);
        if (recipient == null) {
            sender.sendMessage(ComponentColor.red("Player not found."));
            return true;
        }

        if (sender.getUniqueId().equals(recipient.getUniqueId())) {
            sender.sendMessage(ComponentColor.red("You cannot teleport to yourself."));
            return true;
        }

        FLPlayerSession recipientSession = FarLands.getDataHandler().getSession(recipient);

        if (recipientSession.handle.getIgnoreStatus(sender).includesTeleports()) {
            sender.sendMessage("You cannot teleport to this player.");
            return true;
        }

        if (recipientSession.afk)
            sender.sendMessage(ComponentColor.red("This player is AFK, so they may not receive your request."));

        // Everything else is handled here
        TeleportRequest.open(
                "tpa".equals(args[0])
                        ? TeleportRequest.TeleportType.SENDER_TO_RECIPIENT
                        : TeleportRequest.TeleportType.RECIPIENT_TO_SENDER,
                sender,
                recipient
        );
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }
}
