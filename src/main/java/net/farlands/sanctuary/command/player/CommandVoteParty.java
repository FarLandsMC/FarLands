package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.mechanic.Voting;
import org.bukkit.command.CommandSender;

public class CommandVoteParty extends Command {

    public CommandVoteParty() {
        super(
            CommandData.simple(
                "voteparty",
                "Get the number of votes left till the next vote party.",
                "/voteparty"
            )
            .aliases("vp")
            .category(Category.INFORMATIONAL)
         );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int n = FarLands.getMechanicHandler().getMechanic(Voting.class).getVotesUntilParty();
        info(sender, "{:aqua} more vote{0::s} until a vote party.", n);
        return true;
    }
}
