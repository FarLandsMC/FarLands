package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.entity.Player;

public class CommandVoteRewards extends PlayerCommand {

    public CommandVoteRewards() {
        super(Rank.INITIATE, "Enable or disable receiving vote rewards.", "/voterewards", "voterewards");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        flp.acceptVoteRewards = !flp.acceptVoteRewards;
        info(sender, "Vote rewards toggled {}.", flp.acceptVoteRewards ? "on" : "off");
        return true;
    }
}
