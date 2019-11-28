package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "You must set the server spawn in the overworld.");
            return true;
        }
        FarLands.getDataHandler().getPluginData().setSpawn(loc);
        loc.getWorld().setSpawnLocation(loc);
        sender.sendMessage(ChatColor.GREEN + "Server spawn set at your current location.");
        return true;
    }
}
