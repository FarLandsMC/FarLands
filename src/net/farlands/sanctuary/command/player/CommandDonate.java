package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class CommandDonate extends Command {
    public CommandDonate() {
        super(Rank.INITIATE, "List donation costs and perks.", "/donate", "donate");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Send the info
        sendFormatted(
                sender,
                "&(gold)Donations are processed through Tebex. Donations are not required but truly appreciated! " +
                "All donations go towards paying for server related bills. All donations are final, no refunds will be issued.\n" +
                "%0%1:\n&(blue) - Cost: %2\n - %3 homes\n - Commands: %4\n" +
                "%5%6:\n&(blue) - Cost: %7\n - %8 homes\n - 30 minute AFK cooldown\n - Special collectible\n - Commands: %9",
                Rank.DONOR.getColor(),
                Rank.DONOR.getName(),
                Rank.DONOR_COST_STR,
                Rank.DONOR.getHomes(),
                FarLands.getCommandHandler().getCommands().stream()
                        .filter(cmd -> Rank.DONOR.equals(cmd.getMinRankRequirement()))
                        .map(cmd -> "/" + cmd.getName().toLowerCase())
                        .collect(Collectors.joining(", ")),
                Rank.PATRON.getColor(),
                Rank.PATRON.getName(),
                Rank.PATRON_COST_STR,
                Rank.PATRON.getHomes(),
                FarLands.getCommandHandler().getCommands().stream()
                        .filter(cmd -> Rank.PATRON.equals(cmd.getMinRankRequirement()))
                        .map(cmd -> "/" + cmd.getName().toLowerCase())
                        .collect(Collectors.joining(", "))
        );

        // Provide the link (formatting depends on where the command was run from)
        if (sender instanceof Player)
            sendFormatted(sender, "&(gold)Donate here: $(hoverlink,%0,{&(gray)Click to Follow},&(aqua,underline)%0)",
                    FarLands.getFLConfig().donationLink);
        else
            sendFormatted(sender, "&(gold)Donate here: &(aqua)%0", FarLands.getFLConfig().donationLink);

        return true;
    }
}
