package net.farlands.odyssey.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Materials;
import com.kicas.rp.util.Utils;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;

import net.farlands.odyssey.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        if (args.length == 0) {
            sender.openWorkbench(null, true);
            return true;
        }

        Material item = Utils.valueOfFormattedName(args[0], Material.class);

        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Invalid item name: " + args[0]);
            return true;
        }

        int amount = 0;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return true;
            }

            if (amount < 1) {
                sender.sendMessage(ChatColor.RED + "The amount must be greater than or equal to one.");
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
        List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(item, 1));
        Recipe recipe = null;
        if (recipes.isEmpty()) {
            if (sendMessages)
                sender.sendMessage(ChatColor.RED + "This item does not have a recipe.");
            return 0;
        }

        Map<Material, Integer> requirements = new HashMap<>();
        Map<Material, Integer> available = new HashMap<>();
        int craftable = 0, recipeCount = 0;
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
                requirements.put(mat, requirements.getOrDefault(mat, 0) + 1);
            });

            requirements.keySet().forEach(key -> available.put(key, 0));
            sender.getInventory().forEach(stack -> {
                if (stack != null && requirements.containsKey(stack.getType()))
                    available.put(stack.getType(), available.get(stack.getType()) + stack.getAmount());
            });

            craftable = available.entrySet().stream().map(entry -> entry.getValue() /
                    requirements.get(entry.getKey())).min(Integer::compare).orElse(0);

            if (craftable > 0)
                break;
        }

        if (recipeCount == 0)
            return 0;

        final int finalRecipeCount = recipeCount;
        if (craftable < recipeCount) {
            requirements.forEach((key, amt) -> available.put(key, available.get(key) +
                    craft(sender, key, amt * finalRecipeCount - available.get(key), false)));
            craftable = available.entrySet().stream().map(entry -> entry.getValue() /
                    requirements.get(entry.getKey())).min(Integer::compare).orElse(0);
        }

        if (craftable == 0) {
            if (sendMessages) {
                sender.sendMessage(ChatColor.RED + "You are missing the following materials: " + available.entrySet()
                        .stream().filter(entry -> entry.getValue() < requirements.get(entry.getKey()))
                        .map(entry -> Utils.formattedName(entry.getKey()) + " (" + (requirements.get(entry.getKey()) -
                                entry.getValue()) + ")").collect(Collectors.joining(", ")));
            }
            return 0;
        } else if (craftable < recipeCount && sendMessages) {
            sender.sendMessage(ChatColor.RED + "You only had enough resources in your inventory to craft " +
                    craftable + " of this item.");
        } else
            craftable = recipeCount;

        final int finalCraftable = craftable;
        requirements.forEach((mat, amt) -> {
            int total = 0, required = amt * finalCraftable, first;
            while (total < required) {
                ItemStack stack = sender.getInventory().getItem(first = sender.getInventory().first(mat));
                if (stack.getAmount() < required - total) {
                    total += stack.getAmount();
                    sender.getInventory().setItem(first, null);
                } else {
                    stack.setAmount(stack.getAmount() - (required - total));
                    break;
                }
            }
        });

        FLUtils.giveItem(sender, new ItemStack(item, craftable * recipe.getResult().getAmount()), sendMessages);

        return craftable * recipe.getResult().getAmount();
    }
}
