package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class CommandGamemodeImmune extends Command {

    public CommandGamemodeImmune() {
        super(CommandData.withRank(
            "gamemodeimmune",
            "Toggle immunity to gamemode updates for a player's session.",
            "/gamemodeimmune <player>",
            Rank.ADMIN
        ));
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) return error(sender, "Player not found.");
        if (flp.rank.isStaff()) return error(sender, "Staff are already immune to gamemode updates.");

        FLPlayerSession session = flp.getSession();
        if (session == null) return error(sender, "No active session found for %s.", flp.username);

        session.gamemodeImmune = !session.gamemodeImmune;
        session.update(false);

        success(sender, "Gamemode Immunity toggled %s for %s's current session.", session.gamemodeImmune ? "on" : "off", flp.username);
        sender.sendMessage(
            ComponentColor.red("Toggle it %s by running ", session.gamemodeImmune ? "off" : "on")
                .append(ComponentUtils.command("/gamemodeimmune " + flp.username, NamedTextColor.DARK_RED))
                .append(ComponentColor.red("."))
        );

        return true;
    }

    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
            ? FarLands.getDataHandler().getSessions().stream()
            .filter(s -> !s.handle.rank.isStaff())
            .map(s -> s.handle.username)
            .toList()
            : Collections.emptyList();
    }
}
