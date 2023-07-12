package net.farlands.sanctuary.command.player;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.data.struct.IgnoreStatus;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.kicasmads.cs.Utils.filterStartingWith;

public class CommandIgnore extends Command {

    public CommandIgnore() {
        super(
            CommandData.simple(
                    "ignore",
                    "Ignores a player so that you do not see any of their messages.",
                    "/ignore <player> [type]"
                )
                .category(Category.CHAT)
                .aliases(true, "unignore")
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            error(sender, "You must be in-game to use this command.");
            return true;
        }

        if (args.length == 1)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Get the player they're ignoring
        OfflineFLPlayer ignored = FarLands.getDataHandler().getOfflineFLPlayer(args[1]);
        if (ignored == null) {
            return error(sender, "Player not found.");
        }

        // Make sure they're not ignoring themself
        if (flp.uuid.equals(ignored.uuid)) {
            return error(sender, "You cannot ignore or unignore yourself.");
        }

        IgnoreStatus.IgnoreType type = args.length >= 3
                ? Utils.valueOfFormattedName(args[2], IgnoreStatus.IgnoreType.class)
                : IgnoreStatus.IgnoreType.ALL;
        if (type == null) {
            return error(sender, "Invalid ignore type: " + args[2]);
        }

        if ("ignore".equals(args[0])) {
            // You can't ignore staff
            if (ignored.rank.isStaff()) {
                return error(sender, "You cannot ignore a staff member.");
            }

            // The selected type is already set
            if (flp.getIgnoreStatus(ignored).isSet(type)) {
                return error(sender, "You are already ignoring {} from {}", type.toFormattedString(), ignored);
            }

            flp.updateIgnoreStatus(ignored.uuid, type, true);
            success(sender, "You are now ignoring {} from {}.", type.toFormattedString(), ignored);
        } else if ("unignore".equals(args[0])) {
            IgnoreStatus status = flp.getIgnoreStatus(ignored);
            boolean redundant = type == IgnoreStatus.IgnoreType.ALL ? status.includesNone() : !status.isSet(type);
            // The selected type is not set
            if (redundant) {
                return error(
                    sender,
                    "You are already not ignoring {} {}",
                    type == IgnoreStatus.IgnoreType.ALL
                        ? ""
                        : type.toFormattedString() + " from",
                    ignored
                );
            }

            flp.updateIgnoreStatus(ignored.uuid, type, false);
            return error(
                sender,
                "You are no longer ignoring {} {}",
                type == IgnoreStatus.IgnoreType.ALL
                    ? ""
                    : type.toFormattedString() + " from",
                ignored
            );
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1 -> {
                if (alias.equalsIgnoreCase("unignore")){
                    return filterStartingWith(args[0], FarLands.getDataHandler().getOfflineFLPlayer(sender).getIgnoreList());
                }
                return getOnlinePlayers(args[0], sender);
            }
            case 2 -> {
                return filterStartingWith(
                    args[1],
                    Arrays.stream(IgnoreStatus.IgnoreType.values())
                        .map(Utils::formattedName)
                        .toList()
                );
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }
}
