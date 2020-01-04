package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandGod extends PlayerCommand {
    public CommandGod() {
        super(Rank.JR_BUILDER, "Enable or disable god mode.", "/god", "god");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        sender.sendMessage(ChatColor.GOLD + "God mode " + ((flp.god = !flp.god) ? "enabled." : "disabled."));
        return true;
    }
}
