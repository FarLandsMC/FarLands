package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class CommandExtinguish extends PlayerCommand {
    public CommandExtinguish() {
        super(Rank.PATRON, "Put yourself out if you are on fire.", "/extinguish", "extinguish", "ext");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (sender.getFireTicks() > 0) {
            sender.setFireTicks(0);
            sender.playSound(sender.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);
        } else
            sendFormatted(sender, "&(red)You are not on fire right now.");
        return true;
    }
}
