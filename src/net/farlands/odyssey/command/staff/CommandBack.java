package net.farlands.odyssey.command.staff;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.FLUtils;
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
            TextUtils.sendFormatted(sender, "&(red)You have nowhere to teleport back to.");
            return true;
        }
        FLUtils.tpPlayer(sender, backLocation);
        return true;
    }
}
