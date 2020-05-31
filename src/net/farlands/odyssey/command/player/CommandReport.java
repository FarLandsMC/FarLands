package net.farlands.odyssey.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.TextUtils;
import com.kicas.rp.util.Utils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.Rank;

import net.farlands.odyssey.discord.DiscordChannel;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandReport extends Command {
    public CommandReport() {
        super(Rank.INITIATE, "Report a player, glitch, location, and more.", "/report <player|location|glitch|other> <description>",
                "report");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        // Get and check the report type
        ReportType reportType = Utils.valueOfFormattedName(args[0], ReportType.class);
        if (reportType == null) {
            TextUtils.sendFormatted(sender, "&(red)Invalid report type: %0", args[0]);
            return true;
        }

        StringBuilder sb = new StringBuilder();

        // Append info about the reporter
        sb.append("New **").append(args[0]).append("** report from `").append(sender.getName()).append("`\n");

        // If it's a player report add the player's name
        if (reportType == ReportType.PLAYER) {
            if (args.length < 3) {
                TextUtils.sendFormatted(sender, "&(red)Usage: /report player <playerName> <description>");
                return true;
            }

            sb.append("Subject: `").append(args[1]).append("`\n");
        }

        // Add location information
        if (sender instanceof DiscordSender)
            sb.append("Location: `sent from discord.`\n");
        else {
            Location l = ((Player) sender).getLocation();
            sb.append("Location: `/tl ")
                    .append(Math.floor(l.getX()) + 0.5).append(' ')
                    .append((int) l.getY()).append(' ')
                    .append(Math.floor(l.getZ()) + 0.5).append(' ')
                    .append((int) l.getYaw()).append(' ')
                    .append((int) l.getPitch()).append(' ')
                    .append(l.getWorld().getName())
                    .append("`\n");
        }

        // Add the description
        sb.append("Description:\n```").append(joinArgsBeyond("player".equals(args[0]) ? 1 : 0, " ", args)).append("```");

        // Send the report to discord
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.REPORTS, sb.toString());
        if ("glitch".equals(args[0])) {
            FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.DEV_REPORTS, "Glitch/bug report from `" + sender.getName() + "`:" +
                    "```" + joinArgsBeyond(0, " ", args) + "```");
        }

        TextUtils.sendFormatted(sender, "&(green)Report sent.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length == 1)
            return TabCompleterBase.filterStartingWith(args[0], Arrays.stream(ReportType.VALUES).map(Utils::formattedName));
        else if (args.length == 2 && Utils.valueOfFormattedName(args[0], ReportType.class) == ReportType.PLAYER)
            return getOnlinePlayers(args[1], sender);
        else
            return Collections.emptyList();
    }

    private enum ReportType {
        PLAYER, LOCATION, GLITCH, OTHER;

        static final ReportType[] VALUES = values();
    }
}
