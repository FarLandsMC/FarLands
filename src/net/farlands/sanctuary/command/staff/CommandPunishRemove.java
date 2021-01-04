package net.farlands.sanctuary.command.staff;

import com.kicas.rp.util.Pair;
import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Punishment;
import net.minecraft.server.v1_16_R3.Tuple;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.tuple.Triple;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandPunishRemove extends Command {
    private final Map<UUID, Triple<Integer, OfflineFLPlayer, Punishment.PunishmentType>> ranCommandOnce;

    public CommandPunishRemove(){
        super(Rank.JR_BUILDER, "Remove a players punishment from their record", "/removepunish <player> <punishtype>", false, "removepunish", "deletepunish");
        ranCommandOnce = new HashMap<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args){
        Player player = sender instanceof Player ? (Player) sender : null;

        boolean isConfirm = args.length > 0 && "confirm".equals(args[0]) && player != null && ranCommandOnce.containsKey(player.getUniqueId());
        if (args.length < 1) {
            return false;
        }
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);

        if (flp == null && !isConfirm) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        if(isConfirm){
            flp = ranCommandOnce.get(player.getUniqueId()).getMiddle();
        }

        if (!(sender instanceof ConsoleCommandSender) &&
                flp.uuid.equals(FarLands.getDataHandler().getOfflineFLPlayer(sender).uuid)) {
            sendFormatted(sender, "&(red)That was close!");
            return true;
        }


        Punishment.PunishmentType pt;
        if (args.length == 1) { // Get Latest Punishment
            Punishment punishment = flp.isBanned() ? flp.getCurrentPunishment() : flp.getMostRecentPunishmentAll();
            if (punishment == null) {
                sendFormatted(sender, "&(red)This player has no punishments on record.");
                return true;
            }
            pt = punishment.getType();
        } else { // Get Specific Punishment
            pt = Utils.valueOfFormattedName(args[1], Punishment.PunishmentType.class);
            if (pt == null) {
                sendFormatted(sender, "&(red)Invalid punishment type: " + args[1]);
                return true;
            }
        }
        if(player == null || (ranCommandOnce.containsKey(player.getUniqueId()) && "confirm".equalsIgnoreCase(args[0]))){
            if (player != null) {
                pt = ranCommandOnce.get(player.getUniqueId()).getRight();
                FarLands.getScheduler().completeTask(ranCommandOnce.get(player.getUniqueId()).getLeft());
            }
            if (flp.removePunishment(pt)) {
                sendFormatted(sender, "&(gold)Removed punishment {&(aqua)%0} from {&(aqua)%1}'s record.",
                        Utils.formattedName(pt), flp.username);
            } else {
                sendFormatted(sender, "&(red)This player does not have that punishment on record.");
            }

        } else {
            ranCommandOnce.put(player.getUniqueId(), Triple.of(FarLands.getScheduler()
                    .scheduleSyncDelayedTask(() -> ranCommandOnce.remove(player.getUniqueId()), 30L * 20L),
                    flp, pt));

            sendFormatted(sender, "&(red)Are you sure that you want to remove this player's punishment? This will remove it from their record and delete their Evidence Locker.  If you're sure, type {&(dark_red)/removepunish confirm} to remove.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        switch (args.length) {
            case 0:
                return getOnlinePlayers("", sender);
            case 1:
                return getOnlinePlayers(args[0], sender);
            case 2:
                return Arrays.stream(Punishment.PunishmentType.VALUES).map(Utils::formattedName)
                        .filter(a -> a.startsWith(args[1])).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

}
