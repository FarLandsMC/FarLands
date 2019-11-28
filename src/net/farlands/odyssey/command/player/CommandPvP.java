package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandPvP extends PlayerCommand {
    public CommandPvP() {
        super(Rank.INITIATE, "Toggle on and off PvP.", "/pvp", "pvp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayer flp = FarLands.getPDH().getFLPlayer(sender);
        flp.setPvPing(!flp.isPvPing());
        sender.sendMessage(ChatColor.GOLD + "PvP " + (flp.isPvPing() ? "enabled." : "disabled."));
        return true;
    }
}
