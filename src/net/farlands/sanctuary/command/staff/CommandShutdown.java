package net.farlands.sanctuary.command.staff;

import com.google.common.collect.ImmutableMap;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.FLShutdownEvent;
import net.farlands.sanctuary.data.Config;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.TimeInterval;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandShutdown extends Command {
    private static final Map<Integer, String> NOTIFICATION_TIMES = (new ImmutableMap.Builder<Integer, String>())
            .put(3600, "Server restarting in 1 hour.")
            .put(1800, "Server restarting in 30 minutes.")
            .put(600, "Server restarting in 10 minutes.")
            .put(300, "Server restarting in 5 minutes.")
            .put(60, "Server restarting in 1 minute.")
            .put(30, "Server restarting in 30 seconds.")
            .put(10, "Server restarting in 10 seconds.")
            .put(5, "Server restarting in 5...")
            .put(4, "Server restarting in 4...")
            .put(3, "Server restarting in 3...")
            .put(2, "Server restarting in 2...")
            .put(1, "Server restarting in 1...")
            .build();
    private static final List<Integer> DISCORD_NOTIFICATION_TIMES = Arrays.asList(3600, 1800, 600, 60, 10, 3, 2, 1);
    private static final int MAX_DELAY = 8 * 60 * 60; // Can't restart more than 8 hours in advance
    private static final List<String> TYPES = Arrays.asList("stop", "restart", "backup");

    public CommandShutdown() {
        super(Rank.ADMIN, "Stop the server after a certain amount of time.", "/shutdown <stop|restart|backup> <time>",
                "shutdown");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(FarLands.getFLConfig().isScreenSessionNotSet()) {
            sendFormatted(sender, "&(red)The screen session for this server instance is not specified. " +
                    "This command requires that field to run.");
            return true;
        }
        if(args.length < 2 || !TYPES.contains(args[0]))
            return false;
        final int seconds = (int)TimeInterval.parseSeconds(args[1]);
        if(seconds < 0) {
            sendFormatted(sender, "&(red)Invalid time.");
            return true;
        }else if(seconds > MAX_DELAY) {
            sendFormatted(sender, "&(red)You cannot schedule a shutdown more that 8 hours in advance.");
            return true;
        }
        execute0(seconds, args[0]);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length == 1
                ? TYPES.stream().filter(o -> o.startsWith(args[0])).collect(Collectors.toList())
                : Collections.emptyList();
    }

    private static void execute0(final int seconds, final String mode) {
        NOTIFICATION_TIMES.entrySet().stream().filter(e -> e.getKey() <= seconds).forEach(e -> {
            int delay = seconds - e.getKey();
            if(delay > 0)
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> {
                    Logging.broadcastFormatted(e.getValue(), false);
                    if (DISCORD_NOTIFICATION_TIMES.contains(e.getKey()))
                        Logging.broadcastIngameDiscord(e.getValue(), ChatColor.GOLD.getColor());
                }, 20L * delay);
            else
                Logging.broadcastFormatted(e.getValue(), true);
        });
        FarLands.getScheduler().scheduleSyncDelayedTask(() -> {
            Config cfg = FarLands.getFLConfig();
            if("backup".equals(mode)) {
                FarLands.executeScript("backup.sh", cfg.screenSession, System.getProperty("user.home"),
                        System.getProperty("user.dir"), System.getProperty("user.dir") + "-bu", cfg.dedicatedMemory);
            }else if("restart".equals(mode)) { // Regular restart
                FarLands.executeScript("restart.sh", cfg.screenSession, cfg.dedicatedMemory);
            }
            FarLands.getInstance().getServer().getPluginManager().callEvent(new FLShutdownEvent());
        }, 20L * seconds);
    }
}
