package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandToPlayer extends PlayerCommand {
    public CommandToPlayer() {
        super(Rank.JR_BUILDER, "Teleport to a player.", "/to <player>", "to");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        Player player = getPlayer(args[0], sender);
        if(player == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        sender.teleport(player.getLocation());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }
}
