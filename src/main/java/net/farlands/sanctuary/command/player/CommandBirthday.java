package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Birthday;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandBirthday extends Command {
    private static final long WEEK_DURATION = 7L * 24 * 60 * 60 * 1000;
    private static final long MONTH_DURATION = 30L * 24 * 60 * 60 * 1000;

    public CommandBirthday() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Register your birthday, or view upcoming birthdays.",
                "/birthday <register|upcoming> [month/day|week|month]", "birthday", "birthdays");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        if ("register".equalsIgnoreCase(args[0])) {
            if (args.length == 1)
                return false;

            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            if (flp.birthday != null) {
                error(sender, "You have already registered your birthday! Contact a staff " +
                        "member if it is registered incorrectly and should be reset.");
                return true;
            }

            int slash = args[1].indexOf('/');
            if (slash < 0) {
                error(sender, "Please enter your birthday in the form month/day");
                return true;
            }

            String monthString = args[1].substring(0, slash);
            String dayString = args[1].substring(slash + 1);
            int month, day;
            try {
                month = Integer.parseInt(monthString);
            } catch (NumberFormatException ex) {
                error(sender, "Invalid month number: {}. Your birth month should be a number between 1 and 12.", monthString);
                return true;
            }

            try {
                day = Integer.parseInt(dayString);
            } catch (NumberFormatException ex) {
                error(sender, "Invalid date number: {} Your birth date should be a number between 1 and 31.", dayString);
                return true;
            }

            if (month < 1 || month > 12) {
                error(sender, "Your birth month must be between 1 and 12.");
                return true;
            }

            int maxDays;
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.set(2020, month - 1, 1);
                maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            } catch (Exception e) {
                maxDays = 31;
            }

            if (day < 1 || day > maxDays) {
                error(sender, "Your birth date must be between 1 and {} for this month.", maxDays);
                return true;
            }

            flp.birthday = new Birthday(month - 1, day);
            success(sender, "Your birthday has been registered!");
        } else if ("upcoming".equalsIgnoreCase(args[0])) {
            boolean nextMonth = args.length > 1 && "month".equalsIgnoreCase(args[1]);
            long timeDelta = nextMonth ? MONTH_DURATION : WEEK_DURATION;
            List<OfflineFLPlayer> upcoming = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                    .filter(flp -> {
                        if (flp.birthday == null)
                            return false;
                        long timeFromToday = flp.birthday.timeFromToday();
                        return (timeFromToday > 0 && timeFromToday < timeDelta) || flp.birthday.isToday();
                    })
                    .sorted(Comparator.comparingLong(flp -> flp.birthday.timeFromToday()))
                    .toList();

            if (upcoming.isEmpty()) {
                info(sender, "There are no upcoming birthdays within the next {}", nextMonth ? "month" : "week");
                return true;
            }

            TextComponent.Builder c = Component.text().content("Upcoming Birthdays:").color(NamedTextColor.GOLD);
            for (OfflineFLPlayer flp : upcoming) {
                c.append(ComponentColor.gold("\n{}: {:aqua}", flp, flp.birthday.toFormattedString(true)));
            }
            sender.sendMessage(c.build());
        } else
            return false;

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return Arrays.asList("register", "upcoming");

            case 2: {
                if ("upcoming".equalsIgnoreCase(args[0]))
                    return Arrays.asList("week", "month");
                else if (!"register".equalsIgnoreCase(args[0]))
                    return Collections.emptyList();

                List<String> suggestions = new ArrayList<>();
                if (args[1].contains("/")) {
                    int days;
                    try {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(2020, Integer.parseInt(args[1].split("/")[0]) - 1, 1);
                        days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    } catch (Exception e) {
                        days = 31;
                    }
                    for (int i = 1; i <= days; ++i) {
                        suggestions.add(args[1].substring(0, args[1].indexOf('/')) + "/" + i);
                    }
                } else {
                    for (int i = 1; i <= 12; ++i) {
                        suggestions.add(i + "/");
                    }
                }

                return TabCompleterBase.filterStartingWith(args[1], suggestions);
            }

            default:
                return Collections.emptyList();
        }
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false)
            .addSubcommands(
                new SubcommandData("register", "Register your birthday")
                    .addOption(OptionType.STRING, "date", "month/day", true, true)
            )
            .addSubcommandGroups(
                new SubcommandGroupData("upcoming", "View upcoming birthdays")
                    .addSubcommands(
                        new SubcommandData("week", "View upcoming birthdays for the week"),
                        new SubcommandData("month", "View upcoming birthdays for the month")
                    )
            );
    }
}
