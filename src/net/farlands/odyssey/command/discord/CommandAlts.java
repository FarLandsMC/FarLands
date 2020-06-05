package net.farlands.odyssey.command.discord;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class CommandAlts extends DiscordCommand {
    public CommandAlts() {
        super(Rank.JR_BUILDER, "View the alts of a given player.", "/alts <player>", "alts");
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        List<OfflineFLPlayer> alts = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                .filter(otherFlp -> flp.lastIP.equals(otherFlp.lastIP) && !flp.uuid.equals(otherFlp.uuid))
                .collect(Collectors.toList());

        if (alts.isEmpty())
            sendFormatted(sender, "&(gold)This player has no alts.");
        else {
            List<String> banned = alts.stream().filter(OfflineFLPlayer::isBanned).map(flp0 -> flp0.username).collect(Collectors.toList()),
                    unbanned = alts.stream().filter(p -> !p.isBanned()).map(flp0 -> flp0.username).collect(Collectors.toList());
            if (!banned.isEmpty())
                sendFormatted(sender, "&(gold)Banned alts: %0", String.join(", ", banned));
            if (!unbanned.isEmpty())
                sendFormatted(sender, "&(gold)Alts: %0", String.join(", ", unbanned));
        }

        return true;
    }
}
