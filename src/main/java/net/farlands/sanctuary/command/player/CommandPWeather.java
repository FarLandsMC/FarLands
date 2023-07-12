package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
            return error(sender, "Invalid weather type, please specify one of the following: {}", (Object) CustomWeatherType.VALUES);
        }

        // Change the player's weather
        switch (weatherType) {
            case CLEAR -> {
                FarLands.getDataHandler().getOfflineFLPlayer(sender.getUniqueId()).pweather = true;
                sender.setPlayerWeather(WeatherType.CLEAR);
            }
            case RESET -> {
                FarLands.getDataHandler().getOfflineFLPlayer(sender.getUniqueId()).pweather = false;
                sender.resetPlayerWeather();
                return success(sender, "Weather synchronized to world weather.");
            }
        }

        return success(sender,
            "Persoanl weather set.  Use {} to synchronize your weather to the world weather.",
            ComponentUtils.command("/pweather reset")
        );
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
