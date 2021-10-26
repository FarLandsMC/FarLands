package net.farlands.sanctuary.command.staff;

import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandPartyReset extends Command {

    private final Set<OfflineFLPlayer> needsConfirm;

    public CommandPartyReset() {
        super(Rank.BUILDER, Category.STAFF, "Remove all set homes from the party/1.17 world and set logout " +
                "locations from that world to spawn.", "/partyreset [confirm]", "partyreset");
        needsConfirm = new HashSet<>();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        int playersWithHomes = 0;
        int removedHomes = 0;
        int movedPlayers = 0;
        if (
            needsConfirm.contains(FarLands.getDataHandler().getOfflineFLPlayer(sender)) &&
            args.length > 0 &&
            args[0].equalsIgnoreCase("confirm")
        ) {
            for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {
                // Remove all homes in the party world - this does work
                List<Home> partyHomes = flp.homes // Get all homes in party world
                    .stream()
                    .filter(home -> home.getLocation().getWorld().getName().equalsIgnoreCase("farlands"))
                    .collect(Collectors.toList());

                partyHomes.forEach(home -> flp.removeHome(home.getName())); // Remove the homes

                if (partyHomes.size() > 0) ++playersWithHomes; // Add to the amount of players with changed homes
                removedHomes += partyHomes.size(); // Add to the amount of removed homes

                // Teleport them to spawn if needed
                if (flp.lastLocation.asLocation().getWorld().getName().equalsIgnoreCase("farlands")) {
                    flp.moveToSpawn();
                    ++movedPlayers;
                }
            }
            needsConfirm.remove(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            TextUtils.sendFormatted(sender, "&(green)1.17 world reset. " +
                "Removed %0 $(inflect,noun,0,home) from %1 $(inflect,noun,1,player) and " +
                "moved %2 $(inflect,noun,2,player) to spawn.", removedHomes, playersWithHomes, movedPlayers);
        } else {
            TextUtils.sendFormatted(sender, "&(gold)Please confirm this command with {&(aqua)/partyreset confirm}.");
            needsConfirm.add(FarLands.getDataHandler().getOfflineFLPlayer(sender));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return Collections.singletonList("confirm");
    }
}