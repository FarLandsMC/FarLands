package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.LocationWrapper;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandPocket extends PlayerCommand {
    public CommandPocket() {
        super(Rank.INITIATE, Category.TELEPORTING, "Teleport to the pocket world.", "/pocket", "pocket");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(!Worlds.POCKET.enabled) {
            error(sender, "The pocket world is not currently open");
            return true;
        }

        LocationWrapper spawn = FarLands.getDataHandler().getPluginData().pocketSpawn;
        if (spawn == null) {
            error(sender, "Pocket world spawn not set! Please contact an owner, administrator, or developer and notify them of this problem.");
            return true;
        }

        success(sender, "Teleported to the pocket world");
        FLUtils.tpPlayer(sender, spawn.asLocation());
        return true;
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return Collections.emptyList();
    }
}
