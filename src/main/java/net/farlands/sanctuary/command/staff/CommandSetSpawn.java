package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CommandSetSpawn extends PlayerCommand {
    public CommandSetSpawn() {
        super(Rank.ADMIN, "Set the server or pocket world spawn at your current location.", "/setspawn", "setspawn");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        Location loc = sender.getLocation();
        switch(Worlds.getByWorld(loc.getWorld())) {
            case OVERWORLD -> {
                FarLands.getDataHandler().getPluginData().setSpawn(loc);
                success(sender, "Server spawn set at your current location.");
            }
            case POCKET -> {
                FarLands.getDataHandler().getPluginData().setPocketSpawn(loc);
                success(sender, "Pocket world spawn set at your current location.");
            }
            default -> error(sender, "You must set the server spawn in the overworld or pocket world.");
        }
        return true;
    }
}
