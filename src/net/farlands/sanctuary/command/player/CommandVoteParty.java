package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Voting;

import org.bukkit.command.CommandSender;

public class CommandVoteParty extends Command {
    public CommandVoteParty() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "Get the number of votes left till the next vote party.", "/voteparty", "voteparty", "vp");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sendFormatted(sender, "&(gold){&(aqua)%0} more $(inflect,noun,0,vote) until a vote party.",
                FarLands.getMechanicHandler().getMechanic(Voting.class).getVotesUntilParty());
        return true;
    }
}
