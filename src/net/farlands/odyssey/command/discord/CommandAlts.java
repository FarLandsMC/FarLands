package net.farlands.odyssey.command.discord;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        List<OfflineFLPlayer> alts = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                .filter(otherFlp -> flp.lastIP.equals(otherFlp.lastIP)).collect(Collectors.toList());
        if (alts.isEmpty())
            sender.sendMessage(ChatColor.GOLD + "This player has no alts.");
        else {
            List<String> banned = alts.stream().filter(OfflineFLPlayer::isBanned).map(OfflineFLPlayer::getUsername).collect(Collectors.toList()),
                    normal = alts.stream().filter(p -> !p.isBanned()).map(OfflineFLPlayer::getUsername).collect(Collectors.toList());
            if (!banned.isEmpty())
                sender.sendMessage(ChatColor.GOLD + "Banned alts: " + String.join(", ", banned));
            if (!normal.isEmpty())
                sender.sendMessage(ChatColor.GOLD + "Alts: " + String.join(", ", normal));
        }

        return true;
    }
}
