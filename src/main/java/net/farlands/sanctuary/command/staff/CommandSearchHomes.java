package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.Pagination;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandSearchHomes extends PlayerCommand {

    private static final int MAX_RADIUS = 500;

    public CommandSearchHomes() {
        super(Rank.JR_BUILDER, "Search for nearby homes.", "/searchhomes <radius>", "searchhomes");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {
        if (args.length > 0) {
            int radius = parseNumber(args[0], Integer::parseInt, -1);

            if (radius <= 0 || radius > MAX_RADIUS) {
                return error(sender, "Invalid radius. Must be an integer between 1 and {}.", MAX_RADIUS);
            }

            long start = System.currentTimeMillis();
            List<Component> homes = FarLands
                .getDataHandler()
                .getOfflineFLPlayers()
                .stream()
                .flatMap(flp -> flp
                    .homes
                    .stream()
                    .filter(h -> h.getLocation().getWorld().equals(sender.getWorld()))
                    .filter(h -> h.getLocation().distanceSquared(sender.getLocation()) <= radius * radius)
                    .map(h -> ComponentColor.gold(
                             "{} - {}",
                             flp,
                             ComponentUtils.command(
                                 "/home " + h.getName() + flp.username,
                                 h.asComponent(false, false),
                                 ComponentColor.gray("Click to teleport to home.")
                             )
                         )
                    ))
                .toList();
            FarLands.getDebugger().echo("Found %d homes within %d blocks for %s in %dms".formatted(homes.size(), radius, sender.getName(), System.currentTimeMillis() - start));

            if (homes.isEmpty()) {
                return error(sender, "No homes found within {} blocks.", radius);
            }

            Pagination pagination = new Pagination(ComponentColor.gold("{} homes found", homes.size()), "/searchomes " + radius);
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