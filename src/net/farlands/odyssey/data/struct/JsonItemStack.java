package net.farlands.odyssey.data.struct;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.Logging;
import net.minecraft.server.v1_15_R1.MojangsonParser;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class JsonItemStack {
    private String itemName;
    private int count;
    private String nbt;
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
        net.minecraft.server.v1_15_R1.ItemStack tmp = CraftItemStack.asNMSCopy(new ItemStack(Material.valueOf(itemName.toUpperCase()), count));
        try {
            tmp.setTag(MojangsonParser.parse(nbt));
        } catch (CommandSyntaxException ex) {
            Logging.error("Invalid item JSON detected.");
            return;
        }
        stack = CraftItemStack.asBukkitCopy(tmp);
    }
}
