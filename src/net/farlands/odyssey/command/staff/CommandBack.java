package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandBack extends PlayerCommand {
    public CommandBack() {
        super(Rank.JR_BUILDER, "Return to a previous location.", "/back", "back");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Player sender, String[] args) {
        List<Location> backLocations = (List<Location>)FarLands.getDataHandler().getRADH().retrieveAndStoreIfAbsent(
                new ArrayList<Location>(5), "back", sender.getUniqueId().toString());
        if(backLocations.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "You have nowhere to teleport back to.");
            return true;
        }
        FarLands.getDataHandler().getRADH().store(true, "backTPEventIgnore", sender.getUniqueId().toString());
        Utils.tpPlayer(sender, backLocations.remove(backLocations.size() - 1));
        return true;
    }
}
