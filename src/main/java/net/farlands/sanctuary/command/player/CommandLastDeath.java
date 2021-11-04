package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.PlayerDeath;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

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
            sender.sendMessage(ComponentColor.red("You have no recent death on record."));
            return true;
        }

        Location deathLocation = deaths.get(deaths.size()-1).location();
        String s = "You died at " +
            deathLocation.getBlockX() + ", " +
            deathLocation.getBlockY() + ", " +
            deathLocation.getBlockZ() +
            ". A compass will point to this location.";
        sender.sendMessage(ComponentColor.gray(s));
        return true;
    }
}
