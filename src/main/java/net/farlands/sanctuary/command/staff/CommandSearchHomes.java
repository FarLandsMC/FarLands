package net.farlands.sanctuary.command.staff;

import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.Pagination;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandSearchHomes extends PlayerCommand {

    private static final int MAX_RADIUS = 500;

    // Collection of last searches to be more efficient when searching in pages
    //        Search Location, Radius : Homes Lines
    public Map<Pair<Location, Integer>, List<Component>> searchCache = new HashMap<>();
    private static final int CACHE_TIME = 15; // Minutes

    public CommandSearchHomes() {
        super(Rank.JR_BUILDER, "Search for nearby homes.", "/searchhomes <radius>", "searchhomes");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {
        if (args.length > 0) {
            int radius = parseNumber(args[0], Integer::parseInt, -1);

            if (radius <= 0 || radius > MAX_RADIUS) {
                error(sender, "Invalid radius. Must be an integer between 1 and {}.", MAX_RADIUS);
                return true;
            }
            List<Component> homes;
            Pair<Location, Integer> currentSearch = new Pair<>(sender.getLocation(), radius);
            if (searchCache.containsKey(currentSearch)) {
                info(sender, "This does not contain homes created in the last {} minutes.", CACHE_TIME);
                homes = searchCache.get(currentSearch);
            } else {
                homes = FarLands
                    .getDataHandler()
                    .getOfflineFLPlayers()
                    .stream()
                    .flatMap(flp -> flp
                        .homes
                        .stream()
                        .filter(h -> h.getLocation().distanceSquared(sender.getLocation()) <= radius * radius)
                        .map(h -> ComponentColor.gold(flp.username + " - ")
                            .append(
                                ComponentUtils.command(
                                    "/home " + h.getName() + flp.username,
                                    h.asComponent(false, false),
                                    ComponentColor.gray("Click to teleport to home.")
                                )
                            )
                        ))
                    .toList();
                searchCache.put(currentSearch, homes);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> searchCache.remove(currentSearch), 20 * 60 * CACHE_TIME);
            }

            if (homes.isEmpty()) {
                error(sender, "No homes found within {} blocks.", radius);
            }

            Pagination pagination = new Pagination(ComponentColor.gold("Search Homes"), "/searchomes " + radius);
            pagination.addLines(homes);

            if (args.length > 1) {
                int pageNumber = parseNumber(args[1], Integer::parseInt, -1);
                if (pageNumber <= 0 || pageNumber > pagination.numPages()) {
                    error(sender, "Invalid page number. Must be an integer between 1 and {}.", pagination.numPages());
                }
                pagination.sendPage(pageNumber, sender);
            } else {
                pagination.sendPage(1, sender);
            }
            return true;
        }
        return false;
    }
}