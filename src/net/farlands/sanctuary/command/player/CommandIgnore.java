package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;

import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Collections;
import java.util.List;

public class CommandIgnore extends Command {
    public CommandIgnore() {
        super(Rank.INITIATE, "Ignores a player so that you do not see any of their messages.", "/ignore <player>", true, "ignore", "unignore");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            sendFormatted(sender, "&(red)You must be in-game to use this command.");
            return true;
        }

        if (args.length == 1)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Get the player they're ignoring
        OfflineFLPlayer ignored = FarLands.getDataHandler().getOfflineFLPlayer(args[1]);
        if (ignored == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        // Make sure they're not ignoring themself
        if (flp.uuid.equals(ignored.uuid)) {
            sendFormatted(sender, "&(red)You cannot ignore or unignore yourself.");
            return true;
        }

        if ("ignore".equals(args[0])) {
            // You can't ignore staff
            if (ignored.rank.isStaff()) {
                sendFormatted(sender, "&(red)You cannot ignore a staff member.");
                return true;
            }

            // You can't ignore someone more than once
            if (!flp.setIgnoring(ignored.uuid, true)) {
                sendFormatted(sender, "&(red)You are already ignoring this player.");
                return true;
            }

            sendFormatted(sender, "&(green)You are now ignoring &(aqua)%0", ignored.username);
        } else if ("unignore".equals(args[0])) {
            if (!flp.setIgnoring(ignored.uuid, false)) {
                sendFormatted(sender, "&(red)You were not ignoring this player.");
                return true;
            }

            sendFormatted(sender, "&(green)You are no longer ignoring &(aqua)%0", ignored.username);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
