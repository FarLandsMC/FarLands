package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.region.AutumnEvent;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CommandParty extends PlayerCommand {

    private static final Location TREE = new Location(Bukkit.getWorld("world"), 106, 78, -132, 270, 0);

    public CommandParty() {
        super(Rank.INITIATE, "Teleport to a the current server event.", "/party", "party", "event");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (AutumnEvent.isActive())
            if (FarLands.getWorld().equals(player.getWorld()))
                FLUtils.tpPlayer(player, AutumnEvent.getSpawn());
            else
                FLUtils.tpPlayer(player, TREE);
        else
            player.sendMessage(ChatColor.GOLD + "There are no server events active currently.");
        return true;
    }
}