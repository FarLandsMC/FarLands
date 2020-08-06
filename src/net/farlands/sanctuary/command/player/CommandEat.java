package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.minecraft.server.v1_16_R1.FoodMetaData;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CommandEat extends PlayerCommand {
    public CommandEat() {
        super(Rank.SPONSOR, Category.UTILITY, "Eat up food in your inventory instantly to fill your hunger.", "/eat", "eat", "feed");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        int index = 0;
        Inventory inv = sender.getInventory();
        FoodMetaData foodData = ((CraftPlayer) sender).getHandle().getFoodData();
        while (index < inv.getSize() && foodData.foodLevel < 20) {
            ItemStack stack = inv.getItem(index);
            if (stack == null || !stack.getType().isEdible()) {
                ++ index;
                continue;
            }

            // Eat one of the item
            net.minecraft.server.v1_16_R1.ItemStack copy = CraftItemStack.asNMSCopy(stack);
            foodData.a(copy.getItem(), copy);

            // Use the item
            stack.setAmount(stack.getAmount() - 1);
            if (stack.getAmount() == 0) {
                inv.setItem(index, null);
                ++ index;
            }
        }

        sender.updateInventory();
        return true;
    }
}
