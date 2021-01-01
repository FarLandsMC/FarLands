package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
            } catch (Exception ex) {
                sendFormatted(sender, "&(red)Invalid radius.");
                return true;
            }
            int counter = 0;
            StringBuilder message = new StringBuilder("&(green)Found %0 $(inflect,noun,0,home) within "
                    + radius + " blocks of your location");
            for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {
                for (Home home : flp.homes) {
                    if (sender.getLocation().distance(home.getLocation()) < radius) {
                        Location loc = home.getLocation();
                        String line = "\n&(gold)\"" + home.getName() + "\"&(aqua) belonging to " + flp.username + " at "
                                + "$(hovercmd,/chain {gm3} {home " + home.getName() + " " + flp.username + "}"
                                + ",&(gold)Click to go to home,&(gold)" + loc.getBlockX() + " " + loc.getBlockY() + " "
                                + loc.getBlockZ() + ")";
                        message.append(line);
                        counter++;
                    }
                }
            }
            sendFormatted(sender, message.toString(), counter);
            return true;
        }
        return false;
    }
}