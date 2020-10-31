package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPTime extends PlayerCommand {
    public CommandPTime() {
        super(Rank.KNIGHT, Category.PLAYER_SETTINGS_AND_INFO, "Set the time on your personal in-game clock. Note: " +
                "this does not affect server time.", "/ptime <time>", "ptime");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        // Parse the specified time, and update the player's time
        long time;
        if (args[0].matches("\\d+"))
            time = Long.parseLong(args[0]);
        else {
            Time t = FLUtils.safeValueOf(Time::valueOf, args[0].toUpperCase());

            if (t == null) {
                sendFormatted(sender, "&(red)Invalid time, valid times: %0",
                        Arrays.stream(Time.VALUES).map(Utils::formattedName).collect(Collectors.joining(", ")));
                return true;
            } else if (t == Time.RESET) {
                FarLands.getDataHandler().getOfflineFLPlayer(sender.getUniqueId()).ptime = -1;
                sender.resetPlayerTime();
                sendFormatted(sender, "&(green)Clock synchronized to world time.");
                return true;
            }

            time = t.getTicks();
        }

        time = time % 24000;
        FarLands.getDataHandler().getOfflineFLPlayer(sender.getUniqueId()).ptime = time;
        sender.setPlayerTime(time, false);
        sendFormatted(sender, "&(green)Personal time set. Use $(hovercmd,/ptime reset,{&(gray)Click to Run},&(aqua)/ptime reset) " +
                "to synchronize your time to the world time.");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Arrays.stream(Time.VALUES).map(Utils::formattedName))
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
    }
}
