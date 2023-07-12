package net.farlands.sanctuary.command.player;

import com.google.common.collect.ImmutableMap;
import com.kicas.rp.command.TabCompleterBase;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordCompleter;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommandTimeZone extends Command {

    public CommandTimeZone() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO,
              "Register your time zone for others to see or view the current time in a timezone.",
              "/timezone <register|get> <timezone>",
              "timezone"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "register", "set" -> { // Register personal timezone for /stats
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

                if (args.length == 3 && flp.rank.isStaff()) {
                    flp = FarLands.getDataHandler().getOfflineFLPlayer(args[2]);
                    if (flp == null) {
                        return error(sender, "Player not found.");
                    }
                }

                TimeZone tz = getTimeZoneById(args[1]);

                if (tz == null) {
                    return error(sender, "Invalid Time Zone!");
                }
                flp.setTimezone(tz.getID());

                return success(sender, "Time Zone set to {:aqua}! Current time: {:aqua}", tz.getID(), flp.currentTime());
            }
            case "get" -> { // Get the current time at a location
                TimeZone tz = getTimeZoneById(args[1]);

                if (tz == null) {
                    return error(sender, "Invalid Time Zone: {}.", args[1]);
                }

                return info(sender, "Current time in {:aqua} is {:aqua}", tz.getID(), getTime(tz, sender));
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return List.of("register", "set", "get");
            case 2:
                return TabCompleterBase.filterStartingWith(args[1], Arrays.stream(TimeZone.getAvailableIDs()));

            default:
                return Collections.emptyList();
        }
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false)
            .addSubcommands(
                new SubcommandData("set", "Set your timezone")
                    .addOption(OptionType.STRING, "timezone", "The timezone to get", true, true),
                new SubcommandData("get", "Get current time in a timezone")
                    .addOption(OptionType.STRING, "timezone", "The timezone to get", true, true)
            );
    }

    @Override
    public @Nullable Map<String, DiscordCompleter> discordAutocompletion() {
        return ImmutableMap.of(
            "timezone", (option, partial) -> {
                if (option.equals("timezone")) {
                    return Arrays.stream(TimeZone.getAvailableIDs())
                        .filter(s -> s.toLowerCase().contains(partial.toLowerCase()))
                        .toArray(String[]::new);
                }
                return null;
            }
        );
    }

    private TimeZone getTimeZoneById(final String id) {
        String[] ids = TimeZone.getAvailableIDs();
        String tz = Arrays.stream(ids).filter(x -> x.equalsIgnoreCase(id)).findFirst().orElse(null);
        return tz == null ? null : TimeZone.getTimeZone(tz);
    }

    public static String getTime(TimeZone tz, CommandSender sender) {
        DateFormat sdf = SimpleDateFormat.getTimeInstance(
            DateFormat.SHORT,
            sender instanceof Player player ? player.locale() : Locale.getDefault()
        );

        sdf.setTimeZone(tz);

        return sdf.format(new Date());
    }
}
