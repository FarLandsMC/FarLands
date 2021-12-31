package net.farlands.sanctuary.data;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Helper class for deserializing NBT
 * <p>
 * All methods should have a corresponding one in {@link NBTSerializer}
 */
public class NBTDeserializer {

    /**
     * Convert between {@link ItemStack} and {@link ByteArrayBinaryTag}
     */
    public static ItemStack item(BinaryTag nbt) {
        if (nbt instanceof ByteArrayBinaryTag ba) {
            return ItemStack.deserializeBytes(ba.value());
        }
        throw new IllegalArgumentException("nbt is not an item.");
    }

    /**
     * Convert between {@link Collection}<{@link ItemStack}> and {@link ListBinaryTag}
     */
    public static List<ItemStack> itemsList(BinaryTag nbt) {
        if (nbt instanceof ListBinaryTag nbtList) {
            return new ArrayList<>(nbtList.stream().map(NBTDeserializer::item).toList());
        }
        throw new IllegalArgumentException("nbt is not a list.");

    }

    /**
     * Convert between {@link Map}<{@link String}, {@link ItemStack}> and {@link CompoundBinaryTag}
     */
    public static Map<String, ItemStack> itemsMap(BinaryTag nbt) {
        if (nbt instanceof CompoundBinaryTag compound) {
            Map<String, ItemStack> map = new HashMap<>();
            compound.forEach(e -> {
                map.put(e.getKey(), item(e.getValue()));
            });
            return map;
        }
        throw new IllegalArgumentException("nbt is not a compound.");
    }

}
