package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.TeleportRequest;

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
        Player player = getPlayer(args[1], sender);
        if(player == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        if(sender.getUniqueId().equals(player.getUniqueId())) {
            sendFormatted(sender, "&(red)You cannot teleport to yourself.");
            return true;
        }
        FLPlayerSession playerSession = FarLands.getDataHandler().getSession(player);
        if(playerSession.afk)
            sendFormatted(sender, "&(red)This player is AFK, so they may not receive your request.");
        if(playerSession.handle.isIgnoring(sender))
            return true;
        // Everything else is handled here
        playerSession.sendTeleportRequest(sender, "tpa".equals(args[0]) ? TeleportRequest.TeleportType.SENDER_TO_RECIPIENT
                : TeleportRequest.TeleportType.RECIPIENT_TO_SENDER);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
