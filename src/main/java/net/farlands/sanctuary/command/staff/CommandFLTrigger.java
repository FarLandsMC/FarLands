package net.farlands.sanctuary.command.staff;

import com.kicas.rp.command.TabCompleterBase;
import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Voting;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandFLTrigger extends Command {
    public CommandFLTrigger() {
        super(Rank.ADMIN, "Trigger a code event.", "/fltrigger <event> [args]", "fltrigger");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        Event event = Utils.valueOfFormattedName(args[0], Event.class);
        if (event == null) {
            error(sender, "Invalid event: " + args[0]);
            return true;
        }

        if (event == Event.VOTE_PARTY) {
            FarLands.getMechanicHandler().getMechanic(Voting.class).doVoteParty();
            return true;
        }

        if (args.length == 1) {
            error(sender, "Usage: /trigger " + args[0] + " <player>");
            return true;
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(args[1]);
        if (flp == null) {
            error(sender, "Player not found.");
            return true;
        }

        switch (event) {
            case VOTE:
                flp.addVote();
                break;

            case PLAYER_UPDATE:
                flp.updateAll(true);
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        if (args.length == 1)
            return TabCompleterBase.filterStartingWith(args[0], Arrays.stream(Event.VALUES).map(Utils::formattedName));
        else {
            Event event = Utils.valueOfFormattedName(args[0], Event.class);
            if (event != null && event != Event.VOTE_PARTY)
                return getOnlinePlayers(args[1], sender);
        }

        return Collections.emptyList();
    }

    public enum Event {
        VOTE_PARTY, VOTE, PLAYER_UPDATE;

        static final Event[] VALUES = values();
    }
}
