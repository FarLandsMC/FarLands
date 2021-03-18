package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class CommandDonate extends Command {
    public CommandDonate() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "List donation costs and perks.", "/donate", "donate");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String info = ChatColor.GOLD + "Donations are processed through Tebex. Donations are not required but truly appreciated! " +
                "All donations go towards paying for server related bills. All donations are final, no refunds will be issued.\n";

        info += Rank.DONOR.getColor() + Rank.DONOR.getName() + ":\n" +
                ChatColor.BLUE + " - Cost: " + Rank.DONOR_RANK_COSTS[0] + " USD\n" +
                " - " + Rank.DONOR.getHomes() + " homes\n" +
                " - Commands: " + FarLands.getCommandHandler().getCommands().stream()
                    .filter(cmd -> Rank.DONOR.equals(cmd.getMinRankRequirement()))
                    .map(cmd -> "/" + cmd.getName().toLowerCase())
                    .collect(Collectors.joining(", "));

        info += "\n" + Rank.PATRON.getColor() + Rank.PATRON.getName() + ":\n" +
                ChatColor.BLUE + " - Cost: " + Rank.DONOR_RANK_COSTS[1] + " USD\n" +
                " - " + Rank.PATRON.getHomes() + " homes\n" +
                " - 30 minute AFK cooldown\n" +
                " - Special collectible\n" +
                " - Commands: " + FarLands.getCommandHandler().getCommands().stream()
                    .filter(cmd -> Rank.PATRON.equals(cmd.getMinRankRequirement()))
                    .map(cmd -> "/" + cmd.getName().toLowerCase())
                    .collect(Collectors.joining(", "));

        info += "\n" + Rank.SPONSOR.getColor() + Rank.SPONSOR.getName() + ":\n" +
                ChatColor.BLUE + " - Cost: " + Rank.DONOR_RANK_COSTS[2] + " USD\n" +
                " - " + Rank.SPONSOR.getHomes() + " homes\n" +
                " - Commands: " + FarLands.getCommandHandler().getCommands().stream()
                    .filter(cmd -> Rank.SPONSOR.equals(cmd.getMinRankRequirement()))
                    .map(cmd -> "/" + cmd.getName().toLowerCase())
                    .collect(Collectors.joining(", "));

        // Send the info
        sender.sendMessage(info);

        // Provide the link (formatting depends on where the command was run from)
        if (sender instanceof Player)
            sendFormatted(sender, "&(gold)Donate here: $(hoverlink,%0,{&(gray)Click to Follow},&(aqua,underline)%0)",
                    FarLands.getFLConfig().donationLink);
        else if (sender instanceof DiscordSender) {
            ((DiscordSender) sender).sendMessage("Donate here: <" + FarLands.getFLConfig().donationLink + ">", false);
        } else
            sendFormatted(sender, "&(gold)Donate here: &(aqua)%0", FarLands.getFLConfig().donationLink);

        return true;
    }
}
