package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandMend extends PlayerCommand {
    public CommandMend() {
        super(Rank.SPONSOR, Category.UTILITY, "Use your XP to mend the current item in your hand.", "/mend", "mend");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (totalExp(player) <= 0) {
            player.sendMessage(ComponentColor.red("You do not have any exp to mend with."));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("all")) {
            ItemStack[] inventory = player.getInventory().getContents();
            for (int i = inventory.length; --i >= 0; ) {
                if (inventory[i] == null)
                    continue;

                if (mend(player, inventory[i], false))
                    return true;
            }
            return true;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.getType() == Material.AIR) {
            player.sendMessage(ComponentColor.red("Please hold the item you wish to mend."));
            return true;
        }
        mend(player, player.getInventory().getItemInMainHand(), true);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Stream.of("all"))
                : Collections.emptyList();
    }

    private static boolean mend(Player player, ItemStack stack, boolean sendMessages) {
        ItemMeta meta = stack.getItemMeta();
        if (stack.getEnchantmentLevel(Enchantment.MENDING) <= 0 || !(meta instanceof Damageable)) {
            if (sendMessages)
                player.sendMessage(ComponentColor.red("This item cannot be mended. Does it have the mending enchantment?"));
            return false;
        }

        Damageable damageable = (Damageable) meta;
        int repairXp = (damageable.getDamage() + 1) / 2;
        int playerXp = totalExp(player);

        if (repairXp <= 0)
            return false;

        int usedXp = Math.min(playerXp, repairXp);
        damageable.setDamage(Math.max(damageable.getDamage() - usedXp * 2, 0));
        player.giveExp(-usedXp);
        stack.setItemMeta(damageable);

        if (playerXp == usedXp) {
            player.sendMessage(ComponentColor.red("You ran out of exp to mend items with."));
            return true;
        }
        return false;
    }

    private static int totalExp(Player player) {
        int level = player.getLevel();

        int points = (int)(player.getExp() * player.getExpToLevel());

        if (level <= 16)
            return level * level + 6 * level + points;
        if (level <= 31)
            return (int) (2.5 * level * level - 40.5 * level + 360) + points;
        return (int) (4.5 * level * level - 162.5 * level + 2220) + points;
    }
}
