package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.RandomAccessDataHandler;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandCensor extends PlayerCommand {
    public CommandCensor() {
        super(Rank.INITIATE, "Toggle on or off chat censor.", "/censor", "censor");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        FLPlayer flp = FarLands.getPDH().getFLPlayer(player);
        if(!flp.isCensoring()) {
            flp.setCensoring(true);
            player.sendMessage(ChatColor.GOLD + "Chat censor enabled.");
        }else{
            RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
            if(!radh.isCooldownComplete("censor", player.getUniqueId().toString())) {
                radh.removeCooldown("censor", player.getUniqueId().toString());
                flp.setCensoring(false);
                player.sendMessage(ChatColor.GOLD + "Censor disabled. You can re-enable it with /censor.");
            }else{ // Tell them to run the command again to confirm
                radh.setCooldown(30L * 20L, "censor", player.getUniqueId().toString());
                player.sendMessage(ChatColor.RED + "Are you sure you want to disable the chat censor? Confirm with /censor.");
            }
        }
        return true;
    }
}
