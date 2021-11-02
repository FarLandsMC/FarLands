package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Voting;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.command.CommandSender;

public class CommandVoteParty extends Command {

    public CommandVoteParty() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "Get the number of votes left till the next vote party.", "/voteparty", "voteparty", "vp");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int n = FarLands.getMechanicHandler().getMechanic(Voting.class).getVotesUntilParty();
        sender.sendMessage(
            ComponentColor.aqua(n + "")
                .append(ComponentColor.gold(" more "))
                .append(ComponentColor.gold(n == 1 ? "vote" : "votes"))
                .append(ComponentColor.gold(" until a vote party."))
        );
        return true;
    }
}
