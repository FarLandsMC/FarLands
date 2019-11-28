package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandList extends Command {
    public CommandList() {
        super(Rank.INITIATE, "See the players currently online.", "/list", "list", "who");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        final boolean showVanished = sender instanceof DiscordSender ? ((DiscordSender)sender).getChannel().getIdLong() ==
                FarLands.getFLConfig().getDiscordBotConfig().getChannels().get("staffcommands") : Rank.getRank(sender).isStaff();

        Map<Rank, List<String>> players = new HashMap<>(), staff = new HashMap<>();
        online.stream().map(FarLands.getPDH()::getFLPlayer).filter(p -> !p.getRank().isStaff() && !p.isVanished()) // for media
                .forEach(flp -> Utils.getAndPutIfAbsent(players, flp.getRank(), new LinkedList<>()).add(flp.getUsername()));
        online.stream().map(FarLands.getPDH()::getFLPlayer).filter(p -> p.getRank().isStaff() && (!p.isVanished() || showVanished))
                .forEach(flp -> Utils.getAndPutIfAbsent(staff, flp.getRank(), new LinkedList<>()).add(flp.getUsername() +
                        (showVanished && flp.isVanished() ? "*" : "")));

        if(players.size() + staff.size() == 0) {
            sender.sendMessage(ChatColor.RED + "There are no players online currently.");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        int total = players.values().stream().map(Collection::size).reduce(0, Integer::sum) +
                staff.values().stream().map(Collection::size).reduce(0, Integer::sum);
        sb.append(ChatColor.GOLD).append("- ").append(total).append(" Player").append(total != 1 ? "s" : "")
                .append(" Online -\n");
        if(!players.isEmpty()) {
            players.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor()).append(rank.getSymbol())
                    .append(": ").append(ChatColor.GOLD).append(String.join(", ", players.get(rank))).append('\n'));
        }
        if(!staff.isEmpty()) {
            if(!players.isEmpty())
                sb.append(ChatColor.GOLD).append("- Staff -\n");
            staff.keySet().stream().sorted(Rank::specialCompareTo).forEach(rank -> sb.append(rank.getColor()).append(rank.getSymbol())
                    .append(": ").append(ChatColor.GOLD).append(String.join(", ", staff.get(rank))).append('\n'));
        }

        String msg = sb.toString().trim();
        if(msg.contains("*"))
            msg += "\n*These players are vanished.";
        sender.sendMessage(msg);

        return true;
    }
}
