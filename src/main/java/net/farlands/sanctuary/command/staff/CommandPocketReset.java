package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandPocketReset extends Command {

    private final Set<OfflineFLPlayer> needsConfirm;

    public CommandPocketReset() {
        super(Rank.ADMIN, Category.STAFF, "Remove all set homes from the pocket world and set logout " +
                "locations from that world to spawn.", "/resetpocket [confirm]", "resetpocket"); // Not /pocketreset to prevent accidental tab-complete when doing /pocket
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
                    .filter(home -> Worlds.POCKET.matches(home.getLocation().getWorld()))
                    .toList();

                partyHomes.forEach(home -> flp.removeHome(home.getName())); // Remove the homes

                if (partyHomes.size() > 0) ++playersWithHomes; // Add to the amount of players with changed homes
                removedHomes += partyHomes.size(); // Add to the amount of removed homes

                // Teleport them to spawn if needed
                if (Worlds.POCKET.matches(flp.lastLocation.asLocation().getWorld())) {
                    flp.moveToSpawn();
                    ++movedPlayers;
                }
            }
            needsConfirm.remove(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            success(
                sender,
            "Pocket world reset. Removed %s %s from %s %s and moved %s %s to spawn.",
                 removedHomes, removedHomes == 1 ? "home" : "homes",
                 playersWithHomes, playersWithHomes == 1 ? "player" : "players",
                 movedPlayers, movedPlayers == 1 ? "player" : "players"

            );
        } else {
            sender.sendMessage(ComponentColor.gold("Please confirm this command with ").append(ComponentColor.aqua("/resetpocket confirm")));
            needsConfirm.add(FarLands.getDataHandler().getOfflineFLPlayer(sender));
        }


        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if(args.length != 0) return Collections.emptyList();
        return Collections.singletonList("confirm");
    }
}