package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.CommandHandler;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.ReflectionHelper;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class CommandDonate extends Command {
    public CommandDonate() {
        super(Rank.INITIATE, "List donation costs and perks.", "/donate", "donate");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        List<Command> cmds = ((List<Command>) ReflectionHelper.getFieldValue("commands", CommandHandler.class, FarLands.getCommandHandler()));
        sb.append(ChatColor.GOLD).append("Donations are processed through PayPal. Donations are not required but truly appreciated! ")
                .append("All donations go towards paying for server related bills. All donations are final, no refunds will be issued.\n");
        sb.append(Rank.DONOR.getColor()).append(Rank.DONOR.getSymbol()).append(ChatColor.BLUE).append(" - ").append(Rank.DONOR_COST_STR)
                .append(" - ").append(Rank.DONOR.getHomes()).append(" homes")
                .append(" - ").append(cmds.stream().filter(cmd -> Rank.DONOR.equals(cmd.getMinRankRequirement()))
                .map(cmd -> "/" + cmd.getName().toLowerCase()).collect(Collectors.joining(", "))).append('\n');
        sb.append(Rank.PATRON.getColor()).append(Rank.PATRON.getSymbol()).append(ChatColor.BLUE).append(" - ").append(Rank.PATRON_COST_STR)
                .append(" - ").append(Rank.PATRON.getHomes()).append(" homes")
                .append(" - 30m afk cooldown")
                .append(" - ").append(cmds.stream().filter(cmd -> Rank.PATRON.equals(cmd.getMinRankRequirement()))
                .map(cmd -> "/" + cmd.getName().toLowerCase()).collect(Collectors.joining(", ")));
        sender.sendMessage(sb.toString());
        if (sender instanceof Player)
            sendFormatted(sender, "&(gold)Donate here: $(hoverlink,%0,{&(gray)Click to Follow},&(aqua,underline)%0)", FarLands.getFLConfig().donationLink);
        else
            sender.sendMessage(ChatColor.GOLD + "Donate here: " + ChatColor.AQUA + FarLands.getFLConfig().donationLink);
        return true;
    }
}
