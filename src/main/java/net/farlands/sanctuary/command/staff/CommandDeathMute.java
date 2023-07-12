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

public class CommandDeathMute extends Command {

    public CommandDeathMute() {
        super(CommandData.withRank(
            "deathmute",
            "Toggle death mute for a player's session.",
            "/deathmute <player>",
            Rank.BUILDER
        ));
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) return error(sender, "Player not found.");

        FLPlayerSession session = flp.getSession();
        if (session == null) return error(sender, "No active session found for {}.", flp.username);

        session.deathMute = !session.deathMute;

        success(sender, "{} deaths for {}'s current session.", session.deathMute ? "Muted" : "Unmuted", flp);
        if (session.deathMute) {
            sender.sendMessage(
                ComponentColor.red(
                    "Toggle it off by running {}.",
                    ComponentUtils.command("/deathmute " + flp.username, NamedTextColor.DARK_RED)
                )
            );
        }

        return true;
    }

    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
            ? FarLands.getDataHandler().getSessions().stream()
            .map(s -> s.handle.username)
            .toList()
            : Collections.emptyList();
    }
}
