package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandPvP extends PlayerCommand {
    public CommandPvP() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Toggle on and off PvP.", "/pvp <on|off>", "pvp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (args.length == 0) {
            sender.sendMessage(
                ComponentColor.gold(
                        "Your PvP is currently %s. To %s it, run ",
                        flp.pvp ? "on" : "off",
                        flp.pvp ? "disable" : "enable"
                    )
                    .append(ComponentUtils.command("/pvp " + (flp.pvp ? "off" : "on")))
                    .append(Component.text("."))
            );
            return true;
        }

        if (!args[0].equals("off") && !args[0].equals("on")) {
            return false;
        }

        flp.pvp = args[0].equals("on");

        sender.sendMessage(ComponentColor.green("PvP %s.", flp.pvp ? "enabled" : "disabled"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length == 1
                ? TabCompleterBase.filterStartingWith(args[0], Arrays.asList("on", "off"))
                : Collections.emptyList();
    }
}
