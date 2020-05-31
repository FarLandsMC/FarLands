package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;

import org.bukkit.entity.Player;

public class CommandPvP extends PlayerCommand {
    public CommandPvP() {
        super(Rank.INITIATE, "Toggle on and off PvP.", "/pvp", "pvp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        flp.pvp = !flp.pvp;
        TextUtils.sendFormatted(sender, "&(green)PvP %0", flp.pvp ? "enabled." : "disabled.");
        return true;
    }
}
