package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.*;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public class CommandEchest extends PlayerCommand {

    public CommandEchest() {
        super(CommandData
            .simple("echest", "Open your ender chest.", "/echest")
            .category(Category.UTILITY)
            .minimumRank(Rank.DONOR)
            .rankCompare(CommandData.BooleanOperation.OR)
            .playedHoursRequired(12)
            .advancementsRequired(NamespacedKey.minecraft("end/dragon_egg"))
            .craftedItemsRequired(Material.ENDER_CHEST, Material.ENDER_EYE)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sender.openInventory(sender.getEnderChest());
        return true;
    }
}
