package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.Paginate;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSearchHomes extends PlayerCommand {
    public CommandSearchHomes() {
        super(Rank.JR_BUILDER, "Search for nearby homes.", "/searchhomes <radius>", "searchhomes");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {
        if (args.length > 0) {
            int radius;
            try {
                radius = Integer.parseInt(args[0]);
                // Seems like a reasonable radius
                if (radius <= 0 || radius > 500) {
                    sendFormatted(sender, "&(red)Invalid radius. Must be an integer 1-500."); return true;
                }
            } catch (Exception ex) {
                sendFormatted(sender, "&(red)Invalid radius. Must be an integer 1-500."); return true;
            }
            List<String> lines = new ArrayList<>();
            // Build the list of lines to paginate
            for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {
                flp.homes.stream().filter(home -> home.getLocation().getWorld() == sender.getWorld()).forEach(home -> {
                    if (sender.getLocation().distance(home.getLocation()) < radius) {
                        Location loc = home.getLocation();
                        String line = "&(gold)\"" + home.getName() + "\"&(aqua) belonging to " + flp.username + " at "
                                + "$(hovercmd,/chain {gm3} {home " + home.getName() + " " + flp.username + "}"
                                + ",&(gold)Click to go to home,&(gold)" + loc.getBlockX() + " " + loc.getBlockY() + " "
                                + loc.getBlockZ() + ")";
                        lines.add(line);
                    }
                });
            }
            Pair<String, Integer> foo = new Pair<>();
            if (lines.isEmpty()) { // No homes found within provided radius
                sendFormatted(sender, "&(red)Found 0 homes within %0 $(inflect,noun,0,block) " +
                        "of your location.", radius); return true;
            }
            Paginate paginate = new Paginate(lines, "Search Homes", 9, "searchhomes " + radius);
            String toSend = args.length == 1 ? paginate.getPage(1) : paginate.getPage(Integer.parseInt(args[1]));
            if (toSend == null) {
                sendFormatted(sender, "&(red)Invalid page number. Must be between 1 and %0.",
                        paginate.getMaxPage()); return true;
            }
            sendFormatted(sender, toSend);
            return true;
        }
        return false;
    }
}