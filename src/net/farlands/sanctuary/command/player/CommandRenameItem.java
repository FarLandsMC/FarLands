package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
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
            sender.sendMessage(ChatColor.RED + "Please hold the item you wish to rename.");
            return true;
        }

        String rawName = String.join(" ", args);
        String nameNoFormat = Chat.removeColorCodes(rawName);
        if (nameNoFormat.length() > 35) {
            sender.sendMessage(ChatColor.RED + "Item names can be a maximum of 35 characters.");
            return true;
        }

        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Chat.applyColorCodes(rawName));
        stack.setItemMeta(meta);
        ((CraftPlayer) sender).getHandle().levelDown(-1);
        return true;
    }
}
