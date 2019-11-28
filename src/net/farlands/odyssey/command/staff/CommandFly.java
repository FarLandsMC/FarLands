package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandFly extends PlayerCommand {
    public CommandFly() {
        super(Rank.MEDIA, "Enable or disable flight.", "/fly", "fly");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayer flp = FarLands.getPDH().getFLPlayer(sender);
        flp.setFlying(!flp.isFlying());
        sender.sendMessage(ChatColor.GOLD + "Flying " + (flp.isFlying() ? "enabled." : "disabled."));
        return true;
    }
}
