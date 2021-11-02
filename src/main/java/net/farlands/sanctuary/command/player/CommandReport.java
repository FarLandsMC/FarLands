package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandReport extends Command {
    public CommandReport() {
        super(Rank.INITIATE, Category.REPORTS, "Report a player, glitch, location, and more.", "/report <player|location|glitch|other> <description>",
                "report");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        // Get and check the report type
        ReportType reportType = Utils.valueOfFormattedName(args[0], ReportType.class);
        if (reportType == null) {
            sender.sendMessage(ComponentColor.red("&(red)Invalid report type: %s", args[0]));
            return true;
        }

        // Command for teleporting to the location
        String tpCmd;
        if (sender instanceof DiscordSender)
            tpCmd = "`sent from discord.`";
        else {
            Location l = ((Player) sender).getLocation();
            tpCmd = "/tl "+
                (l.getBlockX() + 0.5) + " " +
                (int) l.getY() + " " +
                (l.getBlockZ() + 0.5) + " " +
                (int) l.getYaw() + " " +
                (int) l.getPitch() + " " +
                l.getWorld().getName();
        }

        EmbedBuilder embedBuilder = reportType.toEmbed(
            sender.getName(),
            tpCmd,
            "```" + joinArgsBeyond("player".equals(args[0]) ? 1 : 0, " ", args) + "```"
        );

        // If it's a player report add the player's name
        if (reportType == ReportType.PLAYER) {
            if (args.length < 3) {
                sender.sendMessage(ComponentColor.red("Usage: /report player <playerName> <description>"));
                return true;
            }

            embedBuilder.addField("Subject", "`" + args[1] + "`", false);
        }

        // Send embed to Discord
        FarLands.getDiscordHandler().sendMessageEmbed(DiscordChannel.REPORTS, embedBuilder);

        // If player report, do @here
        if (reportType == ReportType.PLAYER) {
           FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.REPORTS, "@here");
        }

        if ("glitch".equals(args[0])) {
            embedBuilder.setTitle("Bug Report from `" + sender.getName() + "`").clearFields();

            FarLands.getDiscordHandler().sendMessageEmbed(DiscordChannel.DEV_REPORTS, embedBuilder);
        }

        sender.sendMessage(ComponentColor.green("Report sent."));
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
        PLAYER   (0xAA0000), // DARK_RED
        LOCATION (0xFF55FF), // LIGHT_PURPLE
        GLITCH   (0xFFAA00), // GOLD
        OTHER    (0xAAAAAA); // GRAY

        static final ReportType[] VALUES = values();

        private final int embedColor;

        ReportType(int embedColor) {
            this.embedColor = embedColor;
        }

        public EmbedBuilder toEmbed(String username, String teleportCommand, String description) {
            return new EmbedBuilder()
                .setTitle("New **" + Utils.formattedName(this) + "** report from `" + username + "`")
                .setDescription(description)
                .setColor(this.embedColor)
                .addField("Location", "`" + teleportCommand + "`", false);
        }
    }
}
