package net.farlands.sanctuary.data;

import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

/**
 * Helper class for serializing NBT
 * <p>
 * All methods should have a corresponding one in {@link NBTDeserializer}
 */
public class NBTSerializer {

    /**
     * Convert between {@link ItemStack} and {@link ByteArrayBinaryTag}
     */
    public static ByteArrayBinaryTag item(ItemStack stack) {
        return ByteArrayBinaryTag.of(stack.serializeAsBytes());
    }

    /**
     * Convert between {@link Collection}<{@link ItemStack}> and {@link ListBinaryTag}
     */
    public static ListBinaryTag itemsList(Collection<ItemStack> stacks) {
        ListBinaryTag.Builder<ByteArrayBinaryTag> list = ListBinaryTag.builder(ByteArrayBinaryTag.of().type());
        stacks.stream().map(NBTSerializer::item).forEach(list::add);
        return list.build();
    }

    /**
     * Convert between {@link Map}<{@link String}, {@link ItemStack}> and {@link CompoundBinaryTag}
     */
    public static CompoundBinaryTag itemsMap(Map<String, ItemStack> map) {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        map.forEach((k, v) -> nbt.put(k, item(v)));
        return nbt.build();
    }

}
