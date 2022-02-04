package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.mechanic.QuenchingFireAdvancement;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class CommandExtinguish extends PlayerCommand {

    public CommandExtinguish() {
        super(CommandData
            .withRank(
                "extinguish",
                "Extinguish the fire effect if you are currently on fire.",
                "/extinguish",
                Rank.PATRON
            )
            .aliases(false, "ext")
            .rankCompare(CommandData.BooleanOperation.AND)
            .customRequirement(new QuenchingFireAdvancement())
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (sender.getFireTicks() > 0) {
            sender.setFireTicks(0);
            sender.playSound(sender.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);
        } else {
            sender.sendMessage(ComponentColor.red("You are not on fire right now."));
        }
        return true;
    }
}
