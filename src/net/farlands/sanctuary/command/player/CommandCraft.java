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

        // Parse the amount, defaulting to one
        int amount = 1;
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

        Inventory inventory = sender.getInventory();

        // Try to craft the requested amount
        int crafted = 0;
        while (crafted < amount) {
            int additional = takeRequirements(sender, inventory, new Stack<>(), item, true);
            if (additional == 0)
                break;
            crafted += additional;
        }

        give(item, crafted, sender, inventory, true);
        sender.updateInventory();

        if (crafted == 0)
            sender.sendMessage(ChatColor.RED + "You do not have enough resources to craft any of this item.");
        else if (crafted < amount)
            sender.sendMessage(ChatColor.RED + "You only had enough resources to craft " + crafted + " of this item.");

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

    private void give(Material material, int amount, Player recipient, Inventory inventory, boolean sendMessages) {
        while (amount > 0) {
            int stackSize = Math.min(amount, material.getMaxStackSize());
            FLUtils.giveItem(recipient, inventory, recipient.getLocation(), new ItemStack(material, stackSize), sendMessages);
            amount -= stackSize;
        }
    }

    private int takeRequirements(Player sender, Inventory inv, Stack<Material> materialStack, Material material, boolean sendMessages) {
        // Prevent recursion for items like iron ingots and wheat
        if (materialStack.contains(material))
            return 0;
        materialStack.push(material);

        // Try to find recipes for the item
        List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(material, 1));
        if (recipes.isEmpty()) {
            if (sendMessages)
                sendFormatted(sender, "&(red)This item does not have a recipe.");
            materialStack.pop();
            return 0;
        }

        // Copy the contents so we can restore if the take fails
        ItemStack[] originalContents = clonedContents(inv);

        // Look for a recipe variant that we have the necessary items for
        recipeIter:
        for (Recipe recipe : recipes) {
            if (!(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe))
                continue;

            // Figure out the requirements for this recipe
            Collection<RecipeChoice> requirements = recipe instanceof ShapedRecipe
                    ? ((ShapedRecipe) recipe).getChoiceMap().values()
                    : ((ShapelessRecipe) recipe).getChoiceList();

            // Take items for each choice
            requirementIter:
            for (RecipeChoice requirement : requirements) {
                if (requirement == null)
                    continue;

                for (Material choice : ((RecipeChoice.MaterialChoice) requirement).getChoices()) {
                    if (takeOne(sender, inv, materialStack, choice))
                        continue requirementIter;
                }

                // We could not find an item choice that we could satisfy
                inv.setContents(originalContents);
                continue recipeIter;
            }

            // We have fulfilled every requirement
            materialStack.pop();
            return recipe.getResult().getAmount();
        }

        // We could not find a recipe where we could fulfill every requirement
        materialStack.pop();
        return 0;
    }

    private boolean takeOne(Player sender, Inventory inv, Stack<Material> materialStack, Material material) {
        int first = inv.first(material);
        if (first > -1) {
            ItemStack stack = inv.getItem(first);
            stack.setAmount(stack.getAmount() - 1);

            if (stack.getAmount() == 0)
                inv.remove(stack);

            return true;
        }

        int crafted = takeRequirements(sender, inv, materialStack, material, false);
        give(material, crafted - 1, sender, inv, false);
        return crafted > 0;
    }

    private static ItemStack[] clonedContents(Inventory inv) {
        ItemStack[] contents = inv.getContents();
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0;i < contents.length;++ i) {
            ItemStack original = contents[i];
            clone[i] = original == null ? null : original.clone();
        }
        return clone;
    }
}
