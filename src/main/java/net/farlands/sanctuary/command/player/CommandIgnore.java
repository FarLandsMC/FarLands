package net.farlands.sanctuary.command.player;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.IgnoreStatus;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;

import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Collections;
import java.util.List;

public class CommandIgnore extends Command {
    public CommandIgnore() {
        super(Rank.INITIATE, Category.CHAT, "Ignores a player so that you do not see any of their messages.",
                "/ignore <player> [type]", true, "ignore", "unignore");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            sender.sendMessage(ComponentColor.red("You must be in-game to use this command."));
            return true;
        }

        if (args.length == 1)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Get the player they're ignoring
        OfflineFLPlayer ignored = FarLands.getDataHandler().getOfflineFLPlayer(args[1]);
        if (ignored == null) {
            sender.sendMessage(ComponentColor.red("Player not found."));
            return true;
        }

        // Make sure they're not ignoring themself
        if (flp.uuid.equals(ignored.uuid)) {
            sender.sendMessage(ComponentColor.red("You cannot ignore or unignore yourself."));
            return true;
        }

        IgnoreStatus.IgnoreType type = args.length >= 3
                ? Utils.valueOfFormattedName(args[2], IgnoreStatus.IgnoreType.class)
                : IgnoreStatus.IgnoreType.ALL;
        if (type == null) {
            sender.sendMessage(ComponentColor.red("Invalid ignore type: " + args[2]));
            return true;
        }

        if ("ignore".equals(args[0])) {
            // You can't ignore staff
            if (ignored.rank.isStaff()) {
                sender.sendMessage(ComponentColor.red("You cannot ignore a staff member."));
                return true;
            }

            // The selected type is already set
            if (flp.getIgnoreStatus(ignored).isSet(type)) {
                sender.sendMessage(
                    ComponentColor.red("You are already ignoring " + type.toFormattedString() + " from ")
                        .append(ComponentColor.aqua(ignored.username))
                );
                return true;
            }

            flp.updateIgnoreStatus(ignored.uuid, type, true);
            sender.sendMessage(
                ComponentColor.red("You are now ignoring " + type.toFormattedString() + " from ")
                    .append(ComponentColor.aqua(ignored.username))
            );
        } else if ("unignore".equals(args[0])) {
            IgnoreStatus status = flp.getIgnoreStatus(ignored);
            boolean redundant = type == IgnoreStatus.IgnoreType.ALL ? status.includesNone() : !status.isSet(type);
            // The selected type is not set
            if (redundant) {
                String message = type == IgnoreStatus.IgnoreType.ALL
                    ? "&(red)You are already not ignoring "
                    : "&(red)You are already not ignoring %s from ";
                sender.sendMessage(
                    ComponentColor.red(String.format(message, type.toFormattedString()))
                        .append(ComponentColor.aqua(ignored.username))
                );
                return true;
            }

            flp.updateIgnoreStatus(ignored.uuid, type, false);
            String message = type == IgnoreStatus.IgnoreType.ALL
                ? "You are no longer ignoring "
                : "You are no longer ignoring %s from ";
            sender.sendMessage(
                ComponentColor.green(String.format(message, type.toFormattedString()))
                    .append(ComponentColor.aqua(ignored.username))
            );
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
