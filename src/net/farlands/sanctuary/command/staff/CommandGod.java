package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.entity.Player;

public class CommandGod extends PlayerCommand {
    public CommandGod() {
        super(Rank.JR_BUILDER, "Enable or disable god mode.", "/god", "god");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        sendFormatted(sender, "&(gold)God mode %0.", (flp.god = !flp.god) ? "enabled" : "disabled");
        return true;
    }
}
