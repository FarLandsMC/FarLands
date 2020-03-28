package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.FLUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandBack extends PlayerCommand {
    public CommandBack() {
        super(Rank.JR_BUILDER, "Return to a previous location.", "/back", "back");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        List<Location> backLocations = session.backLocations;
        if(backLocations.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "You have nowhere to teleport back to.");
            return true;
        }
        session.setIgnoreTPForBackLocations();
        FLUtils.tpPlayer(sender, backLocations.remove(backLocations.size() - 1));
        return true;
    }
}
