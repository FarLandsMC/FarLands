package net.farlands.sanctuary.data.struct;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.farlands.sanctuary.util.Logging;
import net.minecraft.nbt.MojangsonParser;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

/**
 * An item stack with nbt.
 */
public class JsonItemStack {
    private final String itemName;
    private final int count;
    private final String nbt;
    private transient ItemStack stack;

    public JsonItemStack() {
        this.itemName = null;
        this.count = 0;
        this.nbt = null;
        this.stack = null;
    }

    public ItemStack getStack() {
        if (stack == null)
            genStack();

        return stack;
    }

    private void genStack() {
        net.minecraft.world.item.ItemStack tmp = CraftItemStack.asNMSCopy(new ItemStack(Material.valueOf(itemName.toUpperCase()), count));
        if (nbt != null && !nbt.isEmpty()) {
            try {
                tmp.setTag(MojangsonParser.parse(nbt));
            } catch (CommandSyntaxException ex) {
                Logging.error("Invalid item JSON detected.");
                return;
            }
        }
        stack = CraftItemStack.asBukkitCopy(tmp);
    }
}
