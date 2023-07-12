package net.farlands.sanctuary.command.staff;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandGod extends PlayerCommand {
    public CommandGod() {
        super(Rank.JR_BUILDER, "Enable or disable god mode.", "/god <on|off>", "god");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (args.length > 0) {
            if ("on".equalsIgnoreCase(args[0]))
                flp.god = true;
            else if ("off".equalsIgnoreCase(args[0]))
                flp.god = false;
            flp.updateSessionIfOnline(false);
            info(sender, "God mode {}.", flp.god ? "enabled" : "disabled");
        } else {
            if (flp.flightPreference) {
                info(sender, "God mode enabled, disable it with /god off");
            } else {
                info(sender, "God mode disabled, enable it with /god on");
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Stream.of("on", "off"))
                : Collections.emptyList();
    }
}
