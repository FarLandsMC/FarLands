package net.farlands.odyssey.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.gui.GuiEvidenceLocker;

import org.bukkit.entity.Player;

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
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        if(flp.punishments.isEmpty()) {
            sendFormatted(sender, "&(red)This player has no punishments, thus no evidence locker slots.");
            return true;
        }
        if(FarLands.getDataHandler().isLockerOpen(flp.uuid)) {
            sendFormatted(sender, "&(red)This player\'s evidence locker is already being edited. You must wait for the other editor to finish.");
            return true;
        }
        (new GuiEvidenceLocker(flp)).openGui(sender);
        return true;
    }
}
