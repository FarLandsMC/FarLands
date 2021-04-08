package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Birthday;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.stream.Collectors;

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
                sender.sendMessage(ChatColor.RED + "You have already registered your birthday! Contact a staff " +
                        "member if it is registered incorrectly and should be reset.");
                return true;
            }

            int slash = args[1].indexOf('/');
            if (slash < 0) {
                sender.sendMessage(ChatColor.RED + "Please enter your birthday in the form month/day");
                return true;
            }

            String monthString = args[1].substring(0, slash);
            String dayString = args[1].substring(slash + 1);
            int month, day;
            try {
                month = Integer.parseInt(monthString);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid month number: " + monthString +
                        ". Your birth month should be a number between 1 and 12.");
                return true;
            }

            try {
                day = Integer.parseInt(dayString);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid date number: " + monthString +
                        ". Your birth date should be a number between 1 and 31.");
                return true;
            }

            if (month < 1 || month > 12) {
                sender.sendMessage(ChatColor.RED + "Your birth month must be between 1 and 12.");
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
                sender.sendMessage(ChatColor.RED + "Your birth date must be between 1 and " + maxDays + " for this month.");
                return true;
            }

            flp.birthday = new Birthday(month, day);
            sender.sendMessage(ChatColor.GREEN + "Your birthday has been registered!");
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
                    .collect(Collectors.toList());

            if (upcoming.isEmpty()) {
                sender.sendMessage(ChatColor.GOLD + "There are no upcoming birthdays within the next " + (nextMonth ? "month" : "week"));
                return true;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.GOLD).append("Upcoming birthdays:\n");
            for (OfflineFLPlayer flp : upcoming) {
                sb.append(flp.username).append(": ").append(ChatColor.AQUA).append(flp.birthday.toFormattedString(true)).append(ChatColor.GOLD)
                        .append('\n');
            }
            sender.sendMessage(sb.toString().trim());
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
}
