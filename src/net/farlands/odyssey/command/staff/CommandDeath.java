package net.farlands.odyssey.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.PlayerDeath;

import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        List<PlayerDeath> deaths = FarLands.getDataHandler().getDeaths(uuid);
        if(deaths.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "This player has no deaths on record.");
            return true;
        }

        int death;
        if (args.length < 2) {
            death = deaths.size() - 1;
        } else {
            try {
                death = deaths.size() - Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid death number. If you wish to rollback a death, use " +
                        "/restoredeath.");
                return true;
            }
            if (deaths.size() - 1 < death || death < 0) {
                sender.sendMessage("Death number must be between 1 and " + deaths.size());
                return true;
            }
        }
        Location deathLocation = deaths.get(death).getLocation();
        sender.teleport(deathLocation);
        sendFormatted(sender, "&(gray)Player {&(white)%0} died at " +
                        "$(hovercmd,/tl %1 %2 %3 %4 %5 %6,{&(gray)Click to teleport},&(white)%1 %2 %3 %4 %5 %6)",
                args[0], deathLocation.getX(), deathLocation.getY(), deathLocation.getZ(),
                deathLocation.getYaw(), deathLocation.getPitch(), deathLocation.getWorld().getName());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
