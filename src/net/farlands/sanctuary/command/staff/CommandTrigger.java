package net.farlands.sanctuary.command.staff;

import com.kicas.rp.command.TabCompleterBase;
import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Voting;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandTrigger extends Command {
    public CommandTrigger() {
        super(Rank.ADMIN, "Trigger a code event.", "/trigger <event> [args]", "trigger");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        Event event = Utils.valueOfFormattedName(args[0], Event.class);
        if (event == null) {
            sender.sendMessage(ChatColor.RED + "Invalid event: " + args[0]);
            return true;
        }

        if (event == Event.VOTE_PARTY) {
            FarLands.getMechanicHandler().getMechanic(Voting.class).doVoteParty();
            return true;
        }

        if (args.length == 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /trigger " + args[0] + " <player>");
            return true;
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(args[1]);
        if (flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        switch (event) {
            case VOTE:
                flp.addVote();
                break;

            case PLAYER_UPDATE:
                FLPlayerSession session = flp.getSession();
                if (session == null)
                    flp.update();
                else
                    session.update(true);
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
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
