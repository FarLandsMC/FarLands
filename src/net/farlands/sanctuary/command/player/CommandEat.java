package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.minecraft.world.food.FoodMetaData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandEat extends PlayerCommand {
    private static final List<Material> BLACKLIST = Arrays.asList(
            Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.ROTTEN_FLESH, Material.SPIDER_EYE,
            Material.POISONOUS_POTATO, Material.PUFFERFISH, Material.SUSPICIOUS_STEW, Material.CHORUS_FRUIT
    );

    public CommandEat() {
        super(Rank.SPONSOR, Category.UTILITY, "Eat up food in your inventory instantly to fill your hunger.",
                "/eat [hand]", "eat", "feed");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        boolean hasEaten = false;
        int index = 0;
        Inventory inv = sender.getInventory();
        FoodMetaData foodData = ((CraftPlayer) sender).getHandle().getFoodData();
        if (foodData.getFoodLevel() >= 20) { // getFoodLevel should work but the field is FoodMetaData#a for reference
            sendFormatted(sender, "&(green)You already have full hunger.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("hand")) {
            while (foodData.getFoodLevel() < 20) {
                ItemStack food = sender.getInventory().getItemInMainHand();
                int location = sender.getInventory().getHeldItemSlot();
                // Try both hands
                if (!food.getType().isEdible() || BLACKLIST.contains(food.getType())) {
                    food = sender.getInventory().getItemInOffHand();
                    location = 45; // off hand slot id
                    if (!food.getType().isEdible() || BLACKLIST.contains(food.getType())) {
                        sendFormatted(sender, "&(red)You are not holding anything edible in either of your hands.");
                        return true;
                    }
                }
                // Eat one of the item
                net.minecraft.world.item.ItemStack copy = CraftItemStack.asNMSCopy(food);
                foodData.a(copy.getItem(), copy);

                // Use the item
                food.setAmount(food.getAmount() - 1);
                if (food.getAmount() == 0) {
                    inv.setItem(location, null);
                }
                hasEaten = true;
            }
        }
        while (index < inv.getSize() && foodData.getFoodLevel() < 20) {
            ItemStack stack = inv.getItem(index);
            if (stack == null || !stack.getType().isEdible() || BLACKLIST.contains(stack.getType())) {
                ++index;
                continue;
            }

            // Eat one of the item
            net.minecraft.world.item.ItemStack copy = CraftItemStack.asNMSCopy(stack);
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

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? TabCompleterBase.filterStartingWith(args[0],
                Collections.singletonList("hand")) : Collections.emptyList();
    }
}
