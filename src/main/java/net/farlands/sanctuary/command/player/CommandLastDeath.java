package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.PlayerDeath;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandLastDeath extends PlayerCommand {

    public CommandLastDeath() {
        super(Rank.INITIATE, Category.UTILITY, "Get the coordinates of your most recent death.",
                "/lastdeath", "lastdeath");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {

        UUID uuid = FarLands.getDataHandler().getOfflineFLPlayer(sender).uuid;
        List<PlayerDeath> deaths = FarLands.getDataHandler().getDeaths(uuid);

        if (deaths.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "You have no recent death on record.");  return true;
        }

        Location deathLocation = deaths.get(deaths.size()-1).getLocation();
        sendFormatted(sender, "&(gray)You died at %0, %1, %2. A compass will point toward this location.",
                deathLocation.getBlockX(), deathLocation.getBlockY(), deathLocation.getBlockZ());

        return true;
    }
}
