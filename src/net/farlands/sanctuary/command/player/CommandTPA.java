package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.TeleportRequest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandTPA extends PlayerCommand {
    public CommandTPA() {
        super(Rank.INITIATE, Category.TELEPORTING, "Request to teleport to another player or for another player to " +
                "teleport to you.", "/tpa|/tpahere <player>", true, "tpa", "tpahere");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 1)
            return false;

        if (FarLands.getDataHandler().getSession(sender).outgoingTeleportRequest != null) {
            sender.sendMessage(ChatColor.RED + "You already have an outgoing teleport request.");
            return true;
        }

        Player recipient = getPlayer(args[1], sender);
        if (recipient == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        if (sender.getUniqueId().equals(recipient.getUniqueId())) {
            sendFormatted(sender, "&(red)You cannot teleport to yourself.");
            return true;
        }

        FLPlayerSession recipientSession = FarLands.getDataHandler().getSession(recipient);
        if (recipientSession.afk)
            sendFormatted(sender, "&(red)This player is AFK, so they may not receive your request.");

        if (recipientSession.handle.getIgnoreStatus(sender).includesTeleports())
            return true;

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
