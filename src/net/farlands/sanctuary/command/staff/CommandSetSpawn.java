package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CommandSetSpawn extends PlayerCommand {
    public CommandSetSpawn() {
        super(Rank.ADMIN, "Set the server spawn at your current location.", "/setspawn", "setspawn");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        Location loc = sender.getLocation();
        if(!"world".equals(loc.getWorld().getName())) {
            sendFormatted(sender, "&(red)You must set the server spawn in the overworld.");
            return true;
        }
        FarLands.getDataHandler().getPluginData().setSpawn(loc);
        loc.getWorld().setSpawnLocation(loc);
        sendFormatted(sender, "&(green)Server spawn set at your current location.");
        return true;
    }
}
