package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Materials;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.FLUtils;
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
        super(Rank.PATRON, Category.UTILITY, "Open a crafting window or craft a specific item. Note: you will need " +
                "the required item ingredients to craft it.", "/craft [item] [amount]", "craft");
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
            sendFormatted(sender, "&(red)Invalid item name: %0", args[0]);
            return true;
        }

        // Parse the amount, defaulting to 0 means that the amount of the item given by the recipe will be the actual amount
        int amount = 0;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sendFormatted(sender, "&(red)Invalid amount: %0", args[1]);
                return true;
            }

            if (amount < 1) {
                sendFormatted(sender, "&(red)The amount must be greater than or equal to one.");
                return true;
            }
        }

        craft(sender, item, amount);

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

    private void craft(Player sender, Material item, int amount) {
        craft(sender, new HashSet<>(), item, amount, true);
    }

    private void craft(Player sender, Set<Material> crafted, Material item, int amount, boolean sendMessages) {
        crafted.add(item);
        if (amount <= 0)
            return;

        // Try to find recipes for the item
        List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(item, 1));
        Recipe recipe = null;
        if (recipes.isEmpty()) {
            if (sendMessages)
                sendFormatted(sender, "&(red)This item does not have a recipe.");
            return;
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
            recipeCount = amount == 0 ? 1 : (int) Math.max(Math.ceil((double) amount / r.getResult().getAmount()), 1.0);

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

            if (requirements.keySet().stream().anyMatch(
                    mat -> !mat.name().contains("PLANKS") && mat != Material.STICK && crafted.contains(mat)
            )) {
                recipeCount = 0;
                continue;
            }

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
            return;

        // This is how many times we need to call the recipe to meet the desired item count
        final int finalRecipeCount = recipeCount;

        // If we can't craft enough to meet the desired amount try crafting some of the ingredients
        for (int i = 0; i < 3 && craftable < recipeCount; ++i) {
            requirements.entrySet().stream()
                    // Order it so that the items they have the least of are created first, handles items like anvils
                    // which require both ingots and blocks
                    .sorted(Comparator.comparing(entry -> available.get(entry.getKey())))
                    .forEach(entry -> {
                        craft(
                                sender,
                                crafted,
                                entry.getKey(),
                                entry.getValue() * finalRecipeCount - available.get(entry.getKey()),
                                false
                        );
                    });

            // Recalculate shit to make sure there aren't errors
            available.clear();
            requirements.keySet().forEach(key -> available.put(key, 0));
            sender.getInventory().forEach(stack -> {
                if (stack != null && requirements.containsKey(stack.getType()))
                    available.put(stack.getType(), available.get(stack.getType()) + stack.getAmount());
            });

            craftable = available.entrySet().stream().map(entry -> entry.getValue() /
                    requirements.get(entry.getKey())).min(Integer::compare).orElse(0);
        }

        // Nothing could be crafted
        if (craftable == 0) {
            if (sendMessages) {
                sendFormatted(sender, "&(red)You are missing the following materials: %0",
                        available.entrySet().stream()
                                .filter(entry -> entry.getValue() < requirements.get(entry.getKey()))
                                .map(entry -> Utils.formattedName(entry.getKey()) +
                                        " (" + (requirements.get(entry.getKey()) - entry.getValue()) + ")")
                                .collect(Collectors.joining(", "))
                );
            }

            return;
        } else if (craftable < recipeCount) {
            // Do NOT combine this with the clause above with a `&&`
            if (sendMessages)
                sendFormatted(sender, "&(red)You only had enough resources in your inventory to craft %0 of this item's recipe.", craftable);
        } else
            craftable = recipeCount;

        if (item == Material.SPRUCE_SIGN)
            return;

        // Remove the required items
        Inventory inv = sender.getInventory();
        for (Map.Entry<Material, Integer> entry : requirements.entrySet()) {
            Material mat = entry.getKey();
            Integer amt = entry.getValue();

            int total = 0, required = amt * craftable, first;
            while (total < required) {
                first = inv.first(mat);

                ItemStack stack = inv.getItem(first);

                // Use all of a stack
                if (stack.getAmount() < required - total) {
                    total += stack.getAmount();
                    inv.setItem(first, null);
                }
                // Use part of a stack
                else {
                    stack.setAmount(stack.getAmount() - (required - total));
                    break;
                }
            }
        }

        // Give them the crafted item
        FLUtils.giveItem(sender, new ItemStack(item, craftable * recipe.getResult().getAmount()), sendMessages);
        sender.updateInventory();
    }
}
