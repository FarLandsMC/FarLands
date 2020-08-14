package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.minecraft.server.v1_16_R1.FoodMetaData;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandEat extends PlayerCommand {
    public CommandEat() {
        super(Rank.SPONSOR, Category.UTILITY, "Eat up food in your inventory instantly to fill your hunger.", "/eat", "eat", "feed");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        boolean hasEaten = false;
        int index = 0;
        Inventory inv = sender.getInventory();
        FoodMetaData foodData = ((CraftPlayer) sender).getHandle().getFoodData();
        if (foodData.foodLevel >= 20) {
            sendFormatted(sender, "&(green)You already have full hunger.");
            return true;
        }
        while (index < inv.getSize() && foodData.foodLevel < 20) {
            ItemStack stack = inv.getItem(index);
            if (stack == null || !stack.getType().isEdible()) {
                ++index;
                continue;
            }

            // Eat one of the item
            net.minecraft.server.v1_16_R1.ItemStack copy = CraftItemStack.asNMSCopy(stack);
            foodData.a(copy.getItem(), copy);

            // Use the item
            stack.setAmount(stack.getAmount() - 1);
            if (stack.getAmount() == 0) {
                inv.setItem(index, null);
                ++index;
            }
            hasEaten = true;
        }

        sender.updateInventory();
        if (hasEaten) {
            sendFormatted(sender, "&(green)Your hunger has been filled.");
            sender.playSound(sender.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
        } else
            sendFormatted(sender, "&(red)You didn't have any food to eat.");
        return true;
    }
}
