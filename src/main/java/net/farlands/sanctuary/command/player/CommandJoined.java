package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommandJoined extends Command {

    public CommandJoined() {
        super(
            CommandData.simple(
                    "joined",
                    "See when a player first joined the server.",
                    "/joined [player]"
                    )
                .category(Category.PLAYER_SETTINGS_AND_INFO)
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = args.length == 0
            ? FarLands.getDataHandler().getOfflineFLPlayer(sender)
            : FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);

        if (flp == null) {
            return info(sender, "This player has not joined the server this season.");
        }

        DateFormat sdf = SimpleDateFormat.getDateTimeInstance( // Some localisation can't hurt
            DateFormat.FULL,
            DateFormat.LONG,
            sender instanceof Player player
                ? player.locale()
                : Locale.getDefault()
        );
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return info(sender, "{} joined on {}", flp, sdf.format(new Date(Bukkit.getOfflinePlayer(flp.uuid).getFirstPlayed())));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
