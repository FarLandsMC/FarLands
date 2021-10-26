package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CommandBack extends PlayerCommand {
    public CommandBack() {
        super(Rank.JR_BUILDER, "Return to a previous location.", "/back", "back");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        Location backLocation = FarLands.getDataHandler().getSession(sender).getBackLocation();
        if(backLocation == null) {
            sendFormatted(sender, "&(red)You have nowhere to teleport back to.");
            return true;
        }
        FLUtils.tpPlayer(sender, backLocation);
        return true;
    }
}
