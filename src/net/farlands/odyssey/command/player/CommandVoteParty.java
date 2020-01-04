package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Voting;

import org.bukkit.command.CommandSender;

public class CommandVoteParty extends Command {
    public CommandVoteParty() {
        super(Rank.INITIATE, "Get the number of votes left till the next vote party.", "/voteparty", "voteparty", "vp");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sendFormatted(sender, "&(gold){&(aqua)%0} more $(inflect,noun,0,vote) until a vote party.",
                FarLands.getMechanicHandler().getMechanic(Voting.class).getVotesUntilParty());
        return true;
    }
}
