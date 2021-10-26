package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandSmite extends Command {
    public CommandSmite() {
        super(Rank.ADMIN, "Smite an inferior peasant.", "/smite <peasant>", "smite");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sendFormatted(sender, "&(red)Please specify the peasant you wish to smite.");
            return true;
        }
        Player player = getPlayer(args[0], sender);
        if(player == null) {
            sendFormatted(sender, "&(red)This peasant does not exist.");
            return true;
        }
        player.getWorld().strikeLightning(player.getLocation());
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
