package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommandSeason extends Command {

    public CommandSeason() {
        super(CommandData.simple(
                      "season",
                      "View information about the current season",
                      "/season"
                  )
                  .category(Category.INFORMATIONAL)
                  .aliases("seasoninfo")
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var pd = FarLands.getDataHandler().getPluginData();
        var players = (int) Arrays.stream(Bukkit.getOfflinePlayers()).filter(p -> p.getLastLogin() >= pd.seasonStartTime).count();

        if (sender instanceof DiscordSender ds) {
            ds.sendMessageEmbeds(
                new EmbedBuilder()
                    .setTitle("FarLands v5")
                    .setDescription("Expedition")
                    .addField("Season Started", "<t:%1$d:D> (<t:%1$d:R>)".formatted(pd.seasonStartTime / 1000), false) // seconds
                    .addField("Players Joined", "%,d".formatted(players), false)
                    .setColor(NamedTextColor.LIGHT_PURPLE.value())
                    .setThumbnail(CommandGithub.GH_AVATAR_URL)
                    .build()
            );
        } else {
            DateFormat sdf = SimpleDateFormat.getDateInstance(
                DateFormat.FULL,
                sender instanceof Player player
                    ? player.locale()
                    : Locale.getDefault()
            );
            Component comp = ComponentColor.gold("""
                    FarLands v5: Expedition
                    Season Started: {:aqua} ({:green})
                    Players Joined: {:aqua:%,d}""",
                sdf.format(new Date(pd.seasonStartTime)),
                TimeInterval.formatTime(System.currentTimeMillis() - pd.seasonStartTime, false, TimeInterval.DAY),
                players
            );
            sender.sendMessage(comp);
        }
        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return Collections.emptyList();
    }
}
