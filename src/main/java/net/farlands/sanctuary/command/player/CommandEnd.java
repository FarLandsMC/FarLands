package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.entity.Player;

public class CommandEnd extends PlayerCommand {

    public CommandEnd() {
        super(CommandData.withRank(
                      "end",
                      "Teleport to the end dimension",
                      "/end",
                      Rank.INITIATE
                  )
                  .rankCompare(CommandData.BooleanOperation.AND)
                  .advancementsRequired("end/root")
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        success(sender, "Teleporting...");
        FLUtils.tpPlayer(
            sender,
            Worlds.END.getLocation(100, 49, 0) // Hard-coded location of obsidian platform
        );
        return true;
    }
}
