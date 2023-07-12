package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandReport extends Command {
    public CommandReport() {
        super(Rank.INITIATE, Category.REPORTS, "Report a player, glitch, location, and more.", "/report <player|location|glitch|other> <description>",
                "report");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        if (sender instanceof DiscordSender ds) {
            ds.ephemeral(true);
        }

        // Get and check the report type
        ReportType reportType = Utils.valueOfFormattedName(args[0], ReportType.class);
        if (reportType == null) {
            sender.sendMessage(ComponentColor.red("Invalid report type: {}", args[0]));
            return true;
        }

        // Command for teleporting to the location
        String tpCmd;
        if (sender instanceof DiscordSender)
            tpCmd = "`Sent from Discord`";
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
            OfflineFLPlayer potentialMatch = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);

            embedBuilder.addField(
                "Subject",
                "`" + args[1] + "` (" + (potentialMatch == null ? "No match found" : "Potentially `" + potentialMatch + "`") + ")",
                false
            );
        }

        // Send embed to Discord
        if(reportType == ReportType.PLAYER) {
            // If player report, do @here
            FarLands.getDiscordHandler().getChannel(DiscordChannel.REPORTS)
                .sendMessage("@here")
                .addEmbeds(embedBuilder.build())
                .queue();
        } else {
            FarLands.getDiscordHandler().getChannel(DiscordChannel.REPORTS)
                .sendMessageEmbeds(embedBuilder.build())
                .queue();
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

    @Override
    public @Nullable SlashCommandData discordCommand() {
        Map<ReportType, SubcommandData> subcommandMap = new HashMap<>();
        for (ReportType rt : ReportType.values()) {
            subcommandMap.put(
                rt,
                new SubcommandData(rt.name().toLowerCase(), rt.discordDescription)
            );
        }

        subcommandMap.get(ReportType.PLAYER).addOption(OptionType.STRING, "player-name", "Name of the player to report", true, true);

        subcommandMap.values().forEach(v -> v.addOption(OptionType.STRING, "description", "Reason for reporting", true, true));

        return Commands.slash(this.getName(), this.description)
            .addSubcommands(subcommandMap.values());
    }

    private enum ReportType {
        PLAYER   (0xAA0000, "Report a Player"), // DARK_RED
        LOCATION (0xFF55FF, "Report a Location"), // LIGHT_PURPLE
        GLITCH   (0xFFAA00, "Report a Bug/Glitch"), // GOLD
        OTHER    (0xAAAAAA, "Report something that doesn't fit into the other categories"); // GRAY

        static final ReportType[] VALUES = values();

        private final int embedColor;
        private final String discordDescription;

        ReportType(int embedColor, String discordDescription) {
            this.embedColor = embedColor;
            this.discordDescription = discordDescription;
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
