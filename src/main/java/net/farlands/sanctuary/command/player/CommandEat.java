package net.farlands.sanctuary.command.player;

import com.google.common.collect.ImmutableMap;
import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandEat extends PlayerCommand {
    private static final List<Material> BLACKLIST = Arrays.asList(
            Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.ROTTEN_FLESH, Material.SPIDER_EYE,
            Material.POISONOUS_POTATO, Material.PUFFERFISH, Material.SUSPICIOUS_STEW, Material.CHORUS_FRUIT, Material.CHICKEN, Material.HONEY_BOTTLE
    );

    // No more NMS! >:D
    private static final Map<Material, Integer> FOOD_LEVELS = new ImmutableMap.Builder<Material, Integer>()
        .put(Material.APPLE, 4)
        .put(Material.BAKED_POTATO, 5)
        .put(Material.BEETROOT, 1)
        .put(Material.BEETROOT_SOUP, 6)
        .put(Material.BREAD, 5)
        .put(Material.CARROT, 3)
        .put(Material.COOKED_CHICKEN, 6)
        .put(Material.COOKED_COD, 5)
        .put(Material.COOKED_MUTTON, 6)
        .put(Material.COOKED_PORKCHOP, 8)
        .put(Material.COOKED_RABBIT, 5)
        .put(Material.COOKED_SALMON, 6)
        .put(Material.COOKIE, 2)
        .put(Material.DRIED_KELP, 1)
        .put(Material.GLOW_BERRIES, 2)
        .put(Material.GOLDEN_CARROT, 6)
        .put(Material.HONEY_BOTTLE, 6)
        .put(Material.MELON_SLICE, 2)
        .put(Material.MUSHROOM_STEW, 6)
        .put(Material.POTATO, 1)
        .put(Material.PUMPKIN_PIE, 8)
        .put(Material.RABBIT_STEW, 10)
        .put(Material.BEEF, 3)
        .put(Material.COD, 2)
        .put(Material.MUTTON, 2)
        .put(Material.PORKCHOP, 3)
        .put(Material.RABBIT, 3)
        .put(Material.SALMON, 2)
        .put(Material.COOKED_BEEF, 8)
        .put(Material.SWEET_BERRIES, 2)
        .put(Material.TROPICAL_FISH, 1)
        .build();

    public CommandEat() {
        super(Rank.SPONSOR, Category.UTILITY, "Eat up food in your inventory instantly to fill your hunger.",
                "/eat [hand]", "eat", "feed");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        boolean hasEaten = false;
        int index = 0;
        Inventory inv = sender.getInventory();
        if (sender.getFoodLevel() >= 20) { // getFoodLevel should work but the field is FoodMetaData#a for reference
            sender.sendMessage(ComponentColor.green("You already have full hunger."));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("hand")) {
            while (sender.getFoodLevel() < 20) {
                ItemStack food = sender.getInventory().getItemInMainHand();
                int location = sender.getInventory().getHeldItemSlot();
                // Try both hands
                if (!food.getType().isEdible() || BLACKLIST.contains(food.getType())) {
                    food = sender.getInventory().getItemInOffHand();
                    location = 45; // off hand slot id
                    if (!food.getType().isEdible() || BLACKLIST.contains(food.getType())) {
                        sender.sendMessage(ComponentColor.red("You are not holding anything edible in either of your hands."));
                        return true;
                    }
                }
                // Eat one of the item
                sender.setFoodLevel(sender.getFoodLevel() + FOOD_LEVELS.get(food.getType()));

                // Use the item
                food.setAmount(food.getAmount() - 1);
                if (food.getAmount() == 0) {
                    inv.setItem(location, null);
                }
                hasEaten = true;
            }
        }
        while (index < inv.getSize() && sender.getFoodLevel() < 20) {
            ItemStack stack = inv.getItem(index);
            if (stack == null || !stack.getType().isEdible() || BLACKLIST.contains(stack.getType())) {
                ++index;
                continue;
            }

            // Eat one of the item
            sender.setFoodLevel(sender.getFoodLevel() + FOOD_LEVELS.get(stack.getType()));

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
            sender.sendMessage(ComponentColor.green("Your hunger has been filled."));
            sender.playSound(sender.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
        } else
            sender.sendMessage(ComponentColor.red("You didn't have any food to eat."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? TabCompleterBase.filterStartingWith(args[0],
                Collections.singletonList("hand")) : Collections.emptyList();
    }
}
