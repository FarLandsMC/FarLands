package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.minecraft.server.v1_16_R1.EntityPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandMend extends PlayerCommand {
    public CommandMend() {
        super(Rank.SPONSOR, Category.UTILITY, "Use your XP to mend the current item in your hand.", "/mend", "mend");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        ItemStack stack = sender.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Please hold the item you wish to mend.");
            return true;
        }

        ItemMeta meta = stack.getItemMeta();
        if (stack.getEnchantmentLevel(Enchantment.MENDING) <= 0 || !(meta instanceof Damageable)) {
            sender.sendMessage(ChatColor.RED + "This item cannot be mended.");
            return true;
        }

        Damageable damageable = (Damageable) meta;
        int usedXp = Math.min(totalExp(sender), 2 * damageable.getDamage());
        damageable.setDamage(damageable.getDamage() - usedXp / 2);
        sender.giveExp(-usedXp);
        stack.setItemMeta((ItemMeta) damageable);

        return true;
    }

    private static int totalExp(Player player) {
        int level = player.getLevel();
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        int points = (int) (handle.exp * handle.getExpToLevel());

        if (level <= 16)
            return level * level + 6 * level + points;
        else if (level > 16 && level <= 31)
            return (int) (2.5 * level * level - 40.5 * level + 360) + points;
        else
            return (int) (4.5 * level * level - 162.5 * level + 2220) + points;
    }
}
