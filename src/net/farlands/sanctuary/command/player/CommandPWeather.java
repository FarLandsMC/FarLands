package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;

import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPWeather extends PlayerCommand {
    public CommandPWeather() {
        super(Rank.SAGE, Category.PLAYER_SETTINGS_AND_INFO, "Set your personal in-game weather. Note: this does not " +
                "affect server weather.", "/pweather <clear|reset>", "pweather");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        // Parse and check the weather type
        CustomWeatherType weatherType = Utils.valueOfFormattedName(args[0], CustomWeatherType.class);
        if (weatherType == null) {
            sendFormatted(sender, "&(red)Invalid weather type, please specify one of the following: %0",
                    Arrays.stream(CustomWeatherType.VALUES).map(Utils::formattedName).collect(Collectors.joining(", ")));
            return true;
        }

        // Change the player's weather
        switch (weatherType) {
            case CLEAR:
                sender.setPlayerWeather(WeatherType.CLEAR);
                break;

            case RESET:
                sender.resetPlayerWeather();
                sendFormatted(sender, "&(green)Weather synchronized to world weather.");
                return true;
        }

        sendFormatted(sender, "&(green)Personal weather set. Use $(hovercmd,/pweather reset,{&(gray)Click to Run}" +
                ",&(aqua)/pweather reset) to synchronize your weather to the world weather.");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Arrays.stream(CustomWeatherType.VALUES).map(Utils::formattedName))
                : Collections.emptyList();
    }


    private enum CustomWeatherType {
        CLEAR, RESET;

        static final CustomWeatherType[] VALUES = values();
    }
}
