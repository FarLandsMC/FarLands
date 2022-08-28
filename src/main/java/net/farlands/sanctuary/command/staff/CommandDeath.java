package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.PlayerDeath;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CommandDeath extends PlayerCommand {
    public CommandDeath() {
        super(Rank.JR_BUILDER, "Teleport to a player's death.", "/death <player> [death]", "death");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        UUID uuid = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]).uuid;
        if(uuid == null) {
            return error(sender, "Player not found.");
        }
        List<PlayerDeath> deaths = FarLands.getDataHandler().getDeaths(uuid);
        if(deaths.isEmpty()) {
            return error(sender, "This player has no deaths on record.");
        }

        int death;
        if (args.length < 2) {
            death = deaths.size() - 1;
        } else {
            try {
                death = deaths.size() - Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                return error(sender, "Invalid death number.  If you wish to rollback a death, use /restoredeath");
            }
            if (deaths.size() - 1 < death || death < 0) {
                return error(sender, "Death number must be between 1 and %d", deaths.size());
            }
        }
        Location deathLocation = deaths.get(death).location();
        sender.teleport(deathLocation);
        sender.sendMessage(
            ComponentColor.gray("Player ").append(ComponentColor.white(args[0]))
                .append(ComponentColor.gray(" died at "))
                .append(ComponentUtils.command(
                    String.format("/tl %s %s %s %s %s %s", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ(),
                                  deathLocation.getYaw(), deathLocation.getPitch(), deathLocation.getWorld().getName()),
                    ComponentColor.white(String.format("%s %s %s %s %s %s", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ(),
                                                       deathLocation.getYaw(), deathLocation.getPitch(), deathLocation.getWorld().getName()))
                ))
        );
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }
}
