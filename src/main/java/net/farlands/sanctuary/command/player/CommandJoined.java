package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;

import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CommandJoined extends Command {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("EEEE, MMMM d, yyyy \'at\' H:mm z");

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public CommandJoined() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "See when a player first joined the server.", "/joined [player]", "joined");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = args.length <= 0 ? FarLands.getDataHandler().getOfflineFLPlayer(sender)
            : FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            sender.sendMessage(ComponentColor.gold("This player has never joined the server before."));
            return true;
        }

        sender.sendMessage(ComponentColor.gold(flp.username + " joined on " + SDF.format(new Date(Bukkit.getOfflinePlayer(flp.uuid).getFirstPlayed()))));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
