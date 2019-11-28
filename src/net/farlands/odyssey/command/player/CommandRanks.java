package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.CommandHandler;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.ReflectionHelper;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class CommandRanks extends Command {
    public CommandRanks() {
        super(Rank.INITIATE, "Show all player ranks.", "/ranks", "ranks");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        for(Rank rank : Rank.VALUES) {
            if(Rank.VOTER.equals(rank))
                continue;
            if(rank.specialCompareTo(Rank.PATRON) > 0)
                break;
            sb.append(rank.getColor()).append(rank.getSymbol()).append(ChatColor.BLUE);
            // Specify code for donation-achieved ranks
            if(Rank.DONOR.equals(rank) || Rank.PATRON.equals(rank))
                sb.append(" - ").append(Rank.DONOR.equals(rank) ? Rank.DONOR_COST_STR : Rank.PATRON_COST_STR);
            else{
                int ptr = rank.getPlayTimeRequired();
                if(ptr > 0)
                    sb.append(" - ").append(TimeInterval.formatTime(ptr * 60L * 60L * 1000L, false)).append(" play-time");
            }
            sb.append(" - ").append(rank.getHomes()).append(rank.getHomes() == 1 ? " home" : " homes");
            if(!Rank.INITIATE.equals(rank)) {
                List<String> cmds = ((List<Command>)ReflectionHelper.getFieldValue("commands", CommandHandler.class, FarLands.getCommandHandler()))
                        .stream().filter(cmd -> rank.equals(cmd.getMinRankRequirement())).map(cmd -> "/" + cmd.getName().toLowerCase())
                        .collect(Collectors.toList());
                if(!cmds.isEmpty())
                    sb.append(" - ").append(String.join(", ", cmds));
            }
            sb.append('\n');
        }
        sender.sendMessage(sb.toString());
        return true;
    }
}
