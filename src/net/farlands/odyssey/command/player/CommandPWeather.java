package net.farlands.odyssey.command.player;

import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandPWeather extends PlayerCommand {
    public CommandPWeather() {
        super(Rank.SAGE, "Change your personal in-game weather.", "/pweather <clear|rain|reset>", "pweather");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        if("reset".equalsIgnoreCase(args[0])) {
            sender.resetPlayerWeather();
            sender.sendMessage(ChatColor.GREEN + "Reset weather to world weather.");
            return true;
        }
        WeatherType wt = "clear".equalsIgnoreCase(args[0])
                ? WeatherType.CLEAR
                : ("rain".equalsIgnoreCase(args[0]) ? WeatherType.DOWNFALL : null);
        if(wt == null) {
            sender.sendMessage(ChatColor.RED + "Invalid weather type.");
            return false;
        }
        sender.setPlayerWeather(wt);
        sender.sendMessage(ChatColor.GREEN + "Personal weather set. Use " + ChatColor.AQUA + "/pweather reset" +
                ChatColor.GREEN + " to reset your weather to the world weather.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? Stream.of("clear", "rain", "reset").filter(name -> name.startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
                    .collect(Collectors.toList())
                : Collections.emptyList();
    }
}
