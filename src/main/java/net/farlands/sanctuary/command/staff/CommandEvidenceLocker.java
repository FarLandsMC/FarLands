package net.farlands.sanctuary.command.staff;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.gui.GuiEvidenceLocker;
import net.farlands.sanctuary.mechanic.GeneralMechanics;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandEvidenceLocker extends PlayerCommand {
    public CommandEvidenceLocker() {
        super(Rank.JR_BUILDER, "Open a player's evidence locker.", "/evidencelocker <player>", "evidencelocker", "el");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if(flp == null) {
            return error(sender, "Player not found.");
        }
        if(flp.punishments.isEmpty()) {
            return error(sender, "This player has no punishments, thus no evidence locker slots.");
        }
        if(FarLands.getDataHandler().isLockerOpen(flp.uuid)) {
            return error(sender, "This player's evidence locker is already being edited. You must wait for the other editor to finish.");
        }
        (new GuiEvidenceLocker(flp)).openGui(sender);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        List<String> suggestions = new ArrayList<>();
        if(args.length <= 1) {

            suggestions.addAll(
                GeneralMechanics.recentlyPunished.stream().map(x -> x.username).filter(x -> x.startsWith(args[0])).collect(Collectors.toList())
            );

            suggestions.addAll(getOnlinePlayers("", sender));
            return TabCompleterBase.filterStartingWith(args[0], suggestions);
        }
        return Collections.emptyList();

    }
}
