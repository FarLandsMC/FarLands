package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandRenameItem extends PlayerCommand {
    public CommandRenameItem() {
        super(Rank.SPONSOR, Category.COSMETIC, "Rename an item with color codes.", "/renameitem", "renameitem");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        ItemStack stack = sender.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            sender.sendMessage(ComponentColor.red("Please hold the item you wish to rename."));
            return true;
        }

        String rawName = String.join(" ", args);
        String nameNoFormat = FLUtils.removeColorCodes(rawName);
        if (nameNoFormat.length() > 35) {
            sender.sendMessage(ComponentColor.red("Item names can be a maximum of 35 characters."));
            return true;
        }

        ItemMeta meta = stack.getItemMeta();

        // TODO: Conver this to use adventure components
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(FLUtils.applyColorCodes(Rank.SPONSOR, rawName)));
        stack.setItemMeta(meta);
        if (sender.getGameMode() != GameMode.CREATIVE) {
            sender.giveExpLevels(-1);
        }
        return true;
    }
}
