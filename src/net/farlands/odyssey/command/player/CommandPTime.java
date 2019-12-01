package net.farlands.odyssey.command.player;

import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPTime extends PlayerCommand {
    public CommandPTime() {
        super(Rank.KNIGHT, "Set the time of your personal clock.", "/ptime <time>", "ptime");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        if("reset".equalsIgnoreCase(args[0])) {
            sender.resetPlayerTime();
            sender.sendMessage(ChatColor.GREEN + "Clock synchronized to world time.");
            return true;
        }
        // Parse the specified time, and update the player's time
        long time;
        if(args[0].matches("\\d+"))
            time = Long.parseLong(args[0]);
        else{
            Time t = Utils.safeValueOf(Time::valueOf, args[0].toUpperCase());
            if(t == null) {
                sender.sendMessage(ChatColor.RED + "Invalid time. Valid times: " +
                        String.join(", ", Arrays.stream(Time.VALUES).map(Time::toString).collect(Collectors.toList())));
                return true;
            }
            time = t.getTicks();
        }
        sender.setPlayerTime(time % 24000L, false);
        sendFormatted(sender, "&(green)Personal time set. Use $(hovercmd,/ptime reset,{&(gray)Click to Run},&(aqua)/ptime reset) " +
                "to synchronize your time to the world time.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? Arrays.stream(Time.VALUES).map(Enum::toString)
                    .filter(name -> name.startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
                    .collect(Collectors.toList())
                : Collections.emptyList();
    }

    private enum Time {
        RESET(0L),
        DAY(1000L),
        MIDDAY(6000L),
        NIGHT(13000L),
        MIDNIGHT(18000L),
        SUNRISE(22916L);

        private final long ticks;

        public static final Time[] VALUES = values();
        
        Time(long ticks) {
            this.ticks = ticks;
        }

        public long getTicks() {
            return ticks;
        }
        
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
