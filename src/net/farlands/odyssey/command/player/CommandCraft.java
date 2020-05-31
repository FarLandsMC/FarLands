package net.farlands.odyssey.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Materials;
import com.kicas.rp.util.TextUtils;
import com.kicas.rp.util.Utils;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;

import net.farlands.odyssey.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.stream.Collectors;

public class CommandCraft extends PlayerCommand {
    private static final List<String> CRAFTABLE_MATERIALS = Arrays.stream(Material.values())
            .filter(Materials::hasRecipe).map(Utils::formattedName).collect(Collectors.toList());

    public CommandCraft() {
        super(Rank.PATRON, "Open a crafting window.", "/craft [item] [amount]", "craft");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // No args, open the table
        if (args.length == 0) {
            sender.openWorkbench(null, true);
            return true;
        }

        // Craft a specific material
        Material item = Utils.valueOfFormattedName(args[0], Material.class);
        if (item == null) {
            TextUtils.sendFormatted(sender, "&(red)Invalid item name: %0", args[0]);
            return true;
        }

        // Parse the amount, defaulting to 0 means that the amount of the item given by the recipe will be the actual amount
        int amount = 0;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                TextUtils.sendFormatted(sender, "&(red)Invalid amount: %0", args[1]);
                return true;
            }

            if (amount < 1) {
                TextUtils.sendFormatted(sender, "&(red)The amount must be greater than or equal to one.");
                return true;
            }
        }

        craft(sender, item, amount, true);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return TabCompleterBase.filterStartingWith(args[0], CRAFTABLE_MATERIALS);
            case 2:
                return "64".startsWith(args[1]) ? Collections.singletonList("64") : Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    private int craft(Player sender, Material item, int amount, boolean sendMessages) {
        // Try to find recipes for the item
        List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(item, 1));
        Recipe recipe = null;
        if (recipes.isEmpty()) {
            if (sendMessages)
                TextUtils.sendFormatted(sender, "&(red)This item does not have a recipe.");
            return 0;
        }

        // Look for a recipe variant that we have the necessary items for
        Map<Material, Integer> requirements = new HashMap<>();
        Map<Material, Integer> available = new HashMap<>();
        int craftable = 0; // How many times we can use the chosen recipe
        int recipeCount = 0; // How many times we need to use the recipe to meet the desired item count
        for (Recipe r : recipes) {
            if (!(r instanceof ShapedRecipe || r instanceof ShapelessRecipe))
                continue;

            requirements.clear();
            available.clear();

            recipe = r;
            recipeCount = amount == 0 ? 1 : Math.max(amount / r.getResult().getAmount(), 1);

            (r instanceof ShapedRecipe
                    ? ((ShapedRecipe) r).getChoiceMap().values()
                    : ((ShapelessRecipe) r).getChoiceList()).stream().filter(Objects::nonNull).forEach(choice -> {
                // If there are multiple options for a material, find one that the sender has in their inventory
                Material mat = null;
                for (Material material : ((RecipeChoice.MaterialChoice) choice).getChoices()) {
                    mat = material;
                    String matName = mat.name();
                    if (sender.getInventory().first(mat) > -1 || ((matName.endsWith("_PLANKS") || (matName.endsWith("_LOG") &&
                            !matName.startsWith("STRIPPED"))) && Arrays.stream(sender.getInventory().getContents())
                            .filter(Objects::nonNull).map(ItemStack::getType).map(Material::name)
                            .anyMatch(name -> name.startsWith(matName.substring(0, matName.lastIndexOf('_')))))) {
                        break;
                    }
                }

                // Add the requirement
                requirements.put(mat, requirements.getOrDefault(mat, 0) + 1);
            });

            // Find out how many of the required items the sender actually has
            requirements.keySet().forEach(key -> available.put(key, 0));
            sender.getInventory().forEach(stack -> {
                if (stack != null && requirements.containsKey(stack.getType()))
                    available.put(stack.getType(), available.get(stack.getType()) + stack.getAmount());
            });

            // Calculate how many of the item we can craft
            craftable = available.entrySet().stream().map(entry -> entry.getValue() /
                    requirements.get(entry.getKey())).min(Integer::compare).orElse(0);

            // If we can craft at least one then use this recipe
            if (craftable > 0)
                break;
        }

        // This happens if the recipe crafts more of an item than the player asked for
        if (recipeCount == 0)
            return 0;

        // This is how many times we need to call the recipe to meet the desired item count
        final int finalRecipeCount = recipeCount;

        // If we can't craft enough to meet the desired amount try crafting some of the ingredients
        if (craftable < recipeCount) {
            requirements.forEach((key, amt) -> available.put(key, available.get(key) +
                    craft(sender, key, amt * finalRecipeCount - available.get(key), false)));
            craftable = available.entrySet().stream().map(entry -> entry.getValue() /
                    requirements.get(entry.getKey())).min(Integer::compare).orElse(0);
        }

        // Nothing could be crafted
        if (craftable == 0) {
            if (sendMessages) {
                TextUtils.sendFormatted(sender, "&(red)You are missing the following materials: %0",
                        available.entrySet().stream()
                                .filter(entry -> entry.getValue() < requirements.get(entry.getKey()))
                                .map(entry -> Utils.formattedName(entry.getKey()) +
                                        " (" + (requirements.get(entry.getKey()) - entry.getValue()) + ")")
                                .collect(Collectors.joining(", "))
                );
            }

            return 0;
        } else if (craftable < recipeCount && sendMessages)
            TextUtils.sendFormatted(sender, "&(red)You only had enough resources in your inventory to craft %0 of this item.", craftable);
        else
            craftable = recipeCount;

        // Remove the required items
        final int finalCraftable = craftable;
        requirements.forEach((mat, amt) -> {
            int total = 0, required = amt * finalCraftable, first;
            while (total < required) {
                ItemStack stack = sender.getInventory().getItem(first = sender.getInventory().first(mat));

                // Use all of a stack
                if (stack.getAmount() < required - total) {
                    total += stack.getAmount();
                    sender.getInventory().setItem(first, null);
                }
                // Use part of a stack
                else {
                    stack.setAmount(stack.getAmount() - (required - total));
                    break;
                }
            }
        });

        // Give them the crafted item
        FLUtils.giveItem(sender, new ItemStack(item, craftable * recipe.getResult().getAmount()), sendMessages);

        return craftable * recipe.getResult().getAmount();
    }
}
