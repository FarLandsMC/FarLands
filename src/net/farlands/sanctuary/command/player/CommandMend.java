package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.minecraft.server.v1_16_R1.EntityPlayer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
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
            sendFormatted(player,"&(red)You do not have any exp to mend with.");
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
            sendFormatted(player,"&(red)Please hold the item you wish to mend.");
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
                sendFormatted(player,"&(red)This item cannot be mended. Does it have the mending enchantment?");
            return false;
        }

        Damageable damageable = (Damageable) meta;
        int repairXp = 1 + damageable.getDamage() >> 1, playerXp, usedXp;
        if (repairXp <= 0)
            return false;
        usedXp = Math.min(playerXp = totalExp(player), repairXp); // damage / 2 rounded up
        damageable.setDamage(damageable.getDamage() - (usedXp << 1)); // exp * 2
        player.giveExp(-usedXp);
        if (playerXp == usedXp) {
            sendFormatted(player,"&(red)You ran out of exp to mend items with.");
            return true;
        }
        stack.setItemMeta((ItemMeta) damageable);
        return false;
    }

    private static int totalExp(Player player) {
        int level = player.getLevel();
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        int points = (int) (handle.exp * handle.getExpToLevel());

        if (level <= 16)
            return level * level + 6 * level + points;
        if (level <= 31)
            return (int) (2.5 * level * level - 40.5 * level + 360) + points;
        return (int) (4.5 * level * level - 162.5 * level + 2220) + points;
    }
}
