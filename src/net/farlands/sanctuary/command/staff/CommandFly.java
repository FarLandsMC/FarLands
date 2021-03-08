package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


public class CommandFly extends PlayerCommand {
    public CommandFly() {
        super(Rank.MEDIA, "Enable or disable flight.", "/fly <on|off>", "fly");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (args.length > 0) {
            if ("on".equalsIgnoreCase(args[0]))
                flp.flightPreference = true;
            else if ("off".equalsIgnoreCase(args[0]))
                flp.flightPreference = false;
            flp.updateSessionIfOnline(false);
        }

        sendFormatted(sender, "&(gold)Flying %0.", flp.flightPreference ? "enabled" : "disabled");
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
