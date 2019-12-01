package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandReport extends Command {
    private static final List<String> REPORT_TYPES = Arrays.asList("player", "location", "glitch", "other");

    public CommandReport() {
        super(Rank.INITIATE, "Report a player, glitch, location, and more.",
                "/report <" + String.join("|", REPORT_TYPES) + "> <description>", "report");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0 || args.length < ("player".equals(args[0]) ? 3 : 2) || !REPORT_TYPES.contains(args[0]))
            return false;
        StringBuilder sb = new StringBuilder();
        sb.append("New **").append(args[0]).append("** report from `").append(sender.getName()).append("`\n");
        if("player".equals(args[0]))
            sb.append("Subject: `").append(args[1]).append("`\n");
        if(sender instanceof DiscordSender)
            sb.append("Location: `sent from discord.`\n");
        else{
            Location l = ((Player)sender).getLocation();
            sb.append("Location: `/tl ").append(Math.floor(l.getX()) + 0.5).append(' ').append((int) l.getY()).append(' ')
                    .append(Math.floor(l.getZ()) + 0.5).append(' ').append((int) l.getYaw()).append(' ').append((int) l.getPitch())
                    .append(' ').append(l.getWorld().getName()).append("`\n");
        }
        sb.append("Description:\n```").append(joinArgsBeyond("player".equals(args[0]) ? 1 : 0, " ", args)).append("```");
        FarLands.getDiscordHandler().sendMessageRaw("reports", sb.toString());
        if("glitch".equals(args[0]))
            FarLands.getDiscordHandler().sendMessageRaw("devreports", "Glitch/bug report from `" + sender.getName() + "`:" +
                    "```" + joinArgsBeyond(0, " ", args) + "```");
        sender.sendMessage(ChatColor.GOLD + "Report sent.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if(args.length <= 1)
            return REPORT_TYPES;
        else if("player".equals(args[0]) && args.length == 2)
            return (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args[1]) : getOnlinePlayers(args[1]));
        else
            return Collections.emptyList();
    }
}
