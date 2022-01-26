package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandConfig;
import net.farlands.sanctuary.command.CommandRequirement;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CommandEchest extends PlayerCommand {

    public CommandEchest() {
        super(CommandConfig.builder()
                  .name("echest")
                  .usage("/echest")
                  .description("Open your ender chest.")
                  .category(Category.UTILITY)
                  .requirement(
                      CommandRequirement
                          .builder()
                          .rank(Rank.DONOR)
                          .rankCompare(CommandRequirement.BooleanOperation.AND)
                          .playtimeHours(12)
                          .advancement("end/dragon_egg")
                          .itemsCrafted(Material.ENDER_CHEST, Material.ENDER_EYE)
                          .build()
                  )
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sender.openInventory(sender.getEnderChest());
        return true;
    }
}
