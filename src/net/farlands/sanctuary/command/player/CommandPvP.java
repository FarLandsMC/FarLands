package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.entity.Player;

public class CommandPvP extends PlayerCommand {
    public CommandPvP() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Toggle on and off PvP.", "/pvp", "pvp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        flp.pvp = !flp.pvp;
        sendFormatted(sender, "&(green)PvP %0", flp.pvp ? "enabled." : "disabled.");
        return true;
    }
}
