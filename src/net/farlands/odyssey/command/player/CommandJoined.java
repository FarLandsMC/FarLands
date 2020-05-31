package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;

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
        super(Rank.INITIATE, "See when a player first joined the server.", "/joined [player]", "joined");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = args.length <= 0 ? FarLands.getDataHandler().getOfflineFLPlayer(sender)
                : FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if(flp == null) {
            TextUtils.sendFormatted(sender, "&(gold)This player has never joined the server before.");
            return true;
        }

        TextUtils.sendFormatted(sender, "&(gold)%0 joined on %1", flp.username,
                SDF.format(new Date(Bukkit.getOfflinePlayer(flp.uuid).getFirstPlayed())));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
