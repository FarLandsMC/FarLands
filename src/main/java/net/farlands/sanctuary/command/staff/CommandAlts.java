package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class CommandAlts extends Command {
    public CommandAlts() {
        super(Rank.JR_BUILDER, "View the alts of a given player.", "/alts <player>", "alts");
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            return error(sender, "Player not found.");
        }

        List<OfflineFLPlayer> alts = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                .filter(otherFlp -> flp.lastIP.equals(otherFlp.lastIP) && !flp.uuid.equals(otherFlp.uuid))
                .toList();

        if (alts.isEmpty())
            info(sender, "This player has no alts.");
        else {
            List<String> banned = alts.stream().filter(OfflineFLPlayer::isBanned).map(flp0 -> flp0.username).collect(Collectors.toList()),
                    unbanned = alts.stream().filter(p -> !p.isBanned()).map(flp0 -> flp0.username).collect(Collectors.toList());
            if (!banned.isEmpty())
                info(sender, "Banned Alts: %s", String.join(", ", banned));
            if (!unbanned.isEmpty())
                info(sender, "Alts: %s", String.join(", ", unbanned));
        }

        return true;
    }
}
