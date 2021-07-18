package net.farlands.sanctuary.command.staff;

import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.player.CommandSpawn;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.Logging;
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

        if (needsConfirm.contains(FarLands.getDataHandler().getOfflineFLPlayer(sender)) && args.length > 0
                && args[0].equalsIgnoreCase("confirm")) {
            for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {
                // Remove all homes in the party world - this does work
                flp.homes.stream().filter(home -> home.getLocation().getWorld().getName().equalsIgnoreCase("farlands"))
                        .collect(Collectors.toList()).forEach(home -> flp.removeHome(home.getName()));
                // Teleport them to spawn if needed
                // TODO: 7/17/21 For some reason this isn't working. The player stays in the same place in my testing - Majek
                if (flp.lastLocation.asLocation().getWorld().getName().equalsIgnoreCase("farlands")) {
                    flp.lastLocation = FarLands.getDataHandler().getPluginData().spawn;
                    Logging.error("location set to spawn for " + flp.nickname);
                }
            }
            needsConfirm.remove(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            TextUtils.sendFormatted(sender, "&(green)1.17 world reset.");
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