package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CommandBright extends PlayerCommand {
    public CommandBright() {
        super(Rank.MEDIA, "Toggle on or off brightness.", "/bright", "bright");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (sender.hasPotionEffect(PotionEffectType.NIGHT_VISION))
            sender.removePotionEffect(PotionEffectType.NIGHT_VISION);
        else
            sender.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        return true;
    }
}
