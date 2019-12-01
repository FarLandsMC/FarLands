package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandSeen extends Command {
    public CommandSeen() {
        super(Rank.INITIATE, "Check when a player was last online.", "/seen [player]", "seen");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = args.length <= 0 ? FarLands.getPDH().getFLPlayer(sender) : getFLPlayer(args[0]);
        if (flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        Rank rank = Rank.getRank(sender);
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GREEN).append("Last Seen: ").append(TimeInterval
                .formatTime(System.currentTimeMillis() - flp.getLastLogin(), false));

        if(sender instanceof DiscordSender && ((DiscordSender)sender).getChannel().getIdLong() ==
                FarLands.getFLConfig().getDiscordBotConfig().getChannels().get("staffcommands") ||
                sender instanceof Player && rank.isStaff() || sender instanceof ConsoleCommandSender) {
            sb.append("\nMuted: ").append(flp.isMuted());
            if (!flp.getPunishments().isEmpty()) {
                sb.append("\nPunishments:");
                flp.getPunishments().forEach(p -> sb.append("\n - ").append(p));
            }
            sb.append("\nLast IP: ").append(flp.getLastIP());
        }
        sender.sendMessage(sb.toString());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }
}
