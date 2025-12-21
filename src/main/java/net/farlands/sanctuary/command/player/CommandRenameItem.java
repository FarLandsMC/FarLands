package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.MessageFilter;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandRenameItem extends PlayerCommand {
    public CommandRenameItem() {
        super(
            CommandData.withRank(
                "renameitem",
                "Rename an item with color codes.",
                "/renameitem",
                Rank.SPONSOR
            )
            .category(Category.COSMETIC)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        ItemStack stack = sender.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return error(sender, "Please hold the item you wish to rename.");
        }

        if(sender.getExpToLevel() < 1) {
            return error(sender, "You don't have enough experience to rename an item.");
        }

        String rawName = String.join(" ", args);
        Component name = ComponentUtils.parse(rawName, FarLands.getDataHandler().getOfflineFLPlayer(sender));
        if (ComponentUtils.toText(name).length() > 35) {
            return error(sender, "Item names can be a maximum of 35 characters.");
        }

        if(MessageFilter.INSTANCE.isProfane(ComponentUtils.toText(name))) {
            return error(sender, "You cannot rename an item to this name.");
        }

        ItemMeta meta = stack.getItemMeta();
        if(rawName.isBlank()) {
            meta.displayName(null);
        } else {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        }
        stack.setItemMeta(meta);

        if (sender.getGameMode() != GameMode.CREATIVE) {
            sender.giveExpLevels(-1);
        }
        return true;
    }
}
