package net.farlands.sanctuary.command.staff;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Punishment;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPunish extends Command {

    public CommandPunish() {
        super(Rank.JR_BUILDER, "Punish a player.", "/punish <player> <punishtype> [message]", true, "punish", "ban", "pardon");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < ("pardon".equals(args[0]) || "puniship".equals(args[0]) ? 2 : 3) &&
                !(args.length > 1 && "removepunish".equals(args[0]) && "confirm".equals(args[1])))
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
        if (flp == null) {
            return error(sender, "Player not found");
        }

        if (!(sender instanceof ConsoleCommandSender) &&
                flp.uuid.equals(FarLands.getDataHandler().getOfflineFLPlayer(sender).uuid)) {
            return error(sender, "That was close!");
        }

        Rank senderRank = Rank.getRank(sender);
        if (!"pardon".equals(args[0])) {
            // Staff can punish players, Full staff can punish Jr. staff, Owners can punish all staff except owners.
            if (!(sender instanceof ConsoleCommandSender) && ((senderRank.specialCompareTo(flp.rank) == 0) ||
                    ((senderRank.getPermissionLevel() == 2 || senderRank.getPermissionLevel() == 3) &&
                            (flp.rank.getPermissionLevel() == 2 || flp.rank.getPermissionLevel() == 3)) ||
                    (senderRank.getPermissionLevel() == 1 && flp.rank.getPermissionLevel() > 1))) {
                return error(sender, "You do not have permission to punish this person.");
            }
        }

        switch (args[0]) {
            case "punish", "ban" -> {
                Punishment.PunishmentType pt = Utils.valueOfFormattedName(args[2], Punishment.PunishmentType.class);
                if (pt == null) {
                    return error(sender, "Invalid punishment type: " + args[2]);
                }
                String punishMessage = args.length > 3 ? joinArgsBeyond(2, " ", args) : null;
                if (punishMessage != null && punishMessage.length() > 256) {
                    error(sender, "Punishment messages are limited to 256 characters, it will be truncated.");
                    punishMessage = punishMessage.substring(0, 256);
                }
                punish(sender, flp, pt, punishMessage);
            }
            case "puniship" -> {
                FarLands.getDataHandler().getOfflineFLPlayers().stream()
                    .filter(flp0 -> flp.lastIP.equals(flp0.lastIP))
                    .filter(flp0 -> !flp0.getCurrentPunishment().getType().isPermanent())
                    .forEach(flp0 -> punish(sender, flp0, Punishment.PunishmentType.PERMANENT, null));
            }
            case "pardon" -> {
                Player player = sender instanceof Player ? (Player) sender : null;

                Punishment.PunishmentType pt;
                if (args.length == 2) { // Pardon latest punishment
                    Punishment punishment = flp.isBanned() ? flp.getCurrentPunishment() : flp.getMostRecentPunishment();
                    if (punishment == null) {
                        return error(sender, "This player has no punishments on record.");
                    }
                    pt = punishment.getType();
                } else { // Pardon specific punishment
                    pt = Utils.valueOfFormattedName(args[2], Punishment.PunishmentType.class);
                    if (pt == null) {
                        return error(sender, "Invalid punishment type: " + args[2]);
                    }
                }
                if (flp.pardon(pt)) {
                    success(sender, "Pardoned {} from {:aqua}.", flp, pt);
                } else {
                    error(sender, "This player does not have that punishment on record.");
                }

            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return switch (args.length) {
            case 0 -> getOnlinePlayers("", sender);
            case 1 -> getOnlinePlayers(args[0], sender);
            case 2 -> Arrays.stream(Punishment.PunishmentType.VALUES).map(Utils::formattedName)
                .filter(a -> a.startsWith(args[1])).collect(Collectors.toList());
            default -> Collections.emptyList();
        };
    }

    private void punish(CommandSender sender, OfflineFLPlayer flp, Punishment.PunishmentType pt, String punishMessage) {
        long time = flp.punish(pt, punishMessage);
        info(sender,
             "Punished {} for {}{}. Expires: {:aqua}",
             flp,
             pt,
             punishMessage == null ? "" : " with message \"" + punishMessage + "\"",
             time < 0 ? "Never" : TimeInterval.formatTime(time, false, TimeInterval.MINUTE)
        );

        FarLands.getDiscordHandler().sendMessageRaw(
            DiscordChannel.NOTEBOOK,
            "%s has punished %s for %s%s. Expires: %s".formatted(
                MarkdownProcessor.escapeMarkdown(sender.getName()),
                MarkdownProcessor.escapeMarkdown(flp.username),
                Utils.formattedName(pt),
                punishMessage == null ? "" : " with message `" + punishMessage + "`",
                time < 0L ? "Never" : TimeInterval.formatTime(time, false, TimeInterval.MINUTE)
            )
        );
    }
}
