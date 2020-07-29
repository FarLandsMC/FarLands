package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;

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
    public boolean execute(Player sender, String[] args) {
        if ("gmc".equals(args[0]) && Rank.BUILDER.specialCompareTo(Rank.getRank(sender)) > 0) {
            sendFormatted(sender, "&(red)You do not have permission to use this command.");
            return true;
        }
        boolean isFlying = sender.isFlying();
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        switch (args[0]) {
            case "gmc":
                sender.setGameMode(GameMode.CREATIVE);
                break;
            case "gms":
                sender.setFallDistance(0);
                sender.setGameMode(GameMode.SURVIVAL);
                session.update(false);
                break;
            case "gm3":
                sender.setGameMode(GameMode.SPECTATOR);
                break;
            default:
                if (args.length > 1) {
                    Player targetPlayer = getPlayer(args[1], sender);
                    if (targetPlayer == null) {
                        sendFormatted(sender, "&(red)Player not found.");
                        return true;
                    }
                    sender.setGameMode(GameMode.SPECTATOR);
                    session.update(false);
                    sender.teleport(targetPlayer.getLocation());
                    FarLands.getScheduler().scheduleSyncDelayedTask(() -> sender.setSpectatorTarget(targetPlayer), 20);
                    return true;
                }
                sender.setFallDistance(0);
                sender.setGameMode(GameMode.SPECTATOR.equals(sender.getGameMode()) ? GameMode.SURVIVAL : GameMode.SPECTATOR);
                session.update(false);
        }
        sender.setFlying(isFlying && sender.getAllowFlight());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
