package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandFly extends PlayerCommand {
    public CommandFly() {
        super(Rank.MEDIA, "Enable or disable flight.", "/fly", "fly");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        flp.flightPreference = !flp.flightPreference;
        flp.updateSessionIfOnline(false);
        sender.sendMessage(ChatColor.GOLD + "Flying " + (flp.flightPreference ? "enabled." : "disabled."));
        return true;
    }
}
