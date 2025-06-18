package net.farlands.sanctuary.util;

import com.kicas.rp.util.ReflectionHelper;
import net.kyori.adventure.util.Index;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static net.farlands.sanctuary.util.FLUtils.getCraftBukkitClass;

// TODO: Types for components... somehow

public class ItemUtils {
    /**
     * Convert from a byte[] to an {@link ItemStack}
     */
    public static ItemStack itemStackFromNBT(byte[] bytes) {
        return bytes == null ? null : ItemStack.deserializeBytes(bytes);
    }

    /**
     * Convert from an {@link ItemStack} to a byte[]
     */
    public static byte[] itemStackToNBT(ItemStack stack) {
        return stack.serializeAsBytes();
    }

    /**
     * Give a player an item, optionally send messages
     */
    public static void giveItem(Player player, ItemStack stack, boolean sendMessage) {
        giveItem(player, player.getInventory(), player.getLocation(), stack, sendMessage);
    }

    /**
     * Give a player an item, attempting to place it in the provided inventory and then dropping it at the provided location if full
     */
    public static void giveItem(CommandSender recipient, Inventory inv, Location location, ItemStack stack, boolean sendMessage) {
        if (inv.firstEmpty() > -1) {
            inv.addItem(stack.clone());
        } else {
            location.getWorld().dropItem(location, stack);
            if (sendMessage) {
                recipient.sendMessage(ComponentColor.red("Your inventory was full, so you dropped the item."));
            }
        }
    }

    /**
     * Attempt to apply damage to the item, taking Unbreaking level into account
     * @param item The item to apply the damage (this is mutated)
     * @param amount The amount of damage to attempt to apply
     */
    public static void damageItem(ItemStack item, int amount) {
        if (!(item.getItemMeta() instanceof Damageable dmg)) return;

        for (int i = 0; i < amount; ++i) {
            double chance = 1 / (double) (item.getEnchantmentLevel(Enchantment.UNBREAKING) + 1);
            if (FLUtils.RNG.nextDouble() <= chance) dmg.setDamage(dmg.getDamage() + 1);
        }
        item.setItemMeta(dmg);
        if (dmg.getDamage() >= item.getType().getMaxDurability()) item.setAmount(0);
    }
}
