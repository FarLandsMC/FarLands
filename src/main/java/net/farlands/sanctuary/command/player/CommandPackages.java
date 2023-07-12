package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.PackageToggle;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;


public class CommandPackages extends PlayerCommand {

    public CommandPackages() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "Choose package reception options.", "/packages [accept|ask|decline]", "packages");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        PackageToggle toggle = null;
        if (args.length > 0)
            toggle = Utils.safeValueOf(PackageToggle::valueOf, args[0].toUpperCase());

        if (toggle == null) {
            sender.sendMessage(
                ComponentColor.green("Your packages toggle is currently set to ")
                    .append(ComponentColor.aqua(flp.packageToggle.toString()))
                    .append(ComponentColor.green("."))
            );
            return true;
        }

        flp.packageToggle = toggle;
        sender.sendMessage(ComponentColor.green("Toggle set to " + toggle));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return TabCompleterBase.filterStartingWith(
                args.length > 0 ? args[0] : "",
                Arrays.asList("accept", "ask", "decline")
        );
    }
}
