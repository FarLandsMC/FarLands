package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

import java.util.*;

public class CommandCraft extends PlayerCommand {
    private static final Set<String> CRAFTABLE_MATERIALS = new HashSet<>();
    private static final HashMap<Material, List<Recipe>> RECIPES = new HashMap<>();

    static {
        Iterator<Recipe> itr = Bukkit.recipeIterator();
        while (itr.hasNext()) {
            Recipe recipe = itr.next();
            Material resultType = recipe.getResult().getType();
            CRAFTABLE_MATERIALS.add(Utils.formattedName(resultType));
            List<Recipe> recipes = RECIPES.get(resultType);
            if (recipes == null) {
                recipes = new ArrayList<>();
                recipes.add(recipe);
                RECIPES.put(resultType, recipes);
            } else
                recipes.add(recipe);
        }
    }

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

        long startTime = System.nanoTime();
        Cache cache = new Cache(sender);

        // Try to craft the requested amount
        int crafted = 0;
        while (crafted < amount) {
            int additional = takeRequirements(cache, item, true);
            if (additional == 0)
                break;
            crafted += additional;
        }

        cache.give(item, crafted, true);
        sender.updateInventory();

        if (crafted == 0)
            sender.sendMessage(ChatColor.RED + "You do not have enough resources to craft any of this item.");
        else if (crafted < amount)
            sender.sendMessage(ChatColor.RED + "You only had enough resources to craft " + crafted + " of this item.");

        long elapsed = System.nanoTime() - startTime;
        FarLands.getDebugger().echo("/craft " + String.join(" ", args) + " - Time: " + ((elapsed / 1000) / 1000.0) + "ms");
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

    private int takeRequirements(Cache cache, Material material, boolean sendMessages) {
        // Prevent recursion for items like iron ingots and wheat
        if (cache.materialStack.contains(material))
            return 0;
        cache.pushStack(material);

        // Try to find recipes for the item
        List<Recipe> recipes = RECIPES.get(material);
        if (recipes == null) {
            if (sendMessages)
                sendFormatted(cache.player, "&(red)This item does not have a recipe.");
            cache.popStack();
            return 0;
        }

        // Try to use a cached recipe
        Pair<List<Material>, Integer> cachedRecipe = cache.cachedRecipes.get(material);
        if (cachedRecipe != null) {
            boolean fulfilledAllRequirements = true;
            for (Material requirement : cachedRecipe.getFirst()) {
                if (!takeOne(cache, requirement)) {
                    fulfilledAllRequirements = false;
                    break;
                }
            }

            if (fulfilledAllRequirements) {
                cache.popStack();
                return cachedRecipe.getSecond();
            } else
                cache.restoreInventory();
        }

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
            List<Material> usedChoices = new ArrayList<>(9);
            requirementIter:
            for (RecipeChoice requirement : requirements) {
                if (requirement == null)
                    continue;

                for (Material choice : ((RecipeChoice.MaterialChoice) requirement).getChoices()) {
                    if (takeOne(cache, choice)) {
                        usedChoices.add(choice);
                        continue requirementIter;
                    }
                }

                // We could not find an item choice that we could satisfy
                usedChoices.clear();
                cache.restoreInventory();
                continue recipeIter;
            }

            // We have fulfilled every requirement
            cache.popStack();
            cache.cachedRecipes.put(material, new Pair<>(usedChoices, recipe.getResult().getAmount()));
            return recipe.getResult().getAmount();
        }

        // We could not find a recipe where we could fulfill every requirement
        cache.popStack();
        return 0;
    }

    private boolean takeOne(Cache cache, Material material) {
        Pair<ItemStack, Integer> slot = cache.nextMatching(material);
        int index = slot.getSecond();
        if (index > -1) {
            cache.storeUndo(material, 1);

            ItemStack stack = slot.getFirst();
            stack.setAmount(stack.getAmount() - 1);

            if (stack.getAmount() <= 0)
                cache.inventory.remove(stack);

            return true;
        }

        int crafted = takeRequirements(cache, material, false);
        cache.give(material, crafted - 1, false);
        return crafted > 0;
    }

    private static class Cache {
        Player player;
        Inventory inventory;
        Stack<Material> materialStack;
        Stack<HashMap<Material, Integer>> changeStack;
        HashMap<Material, Integer> cachedSlots;
        HashMap<Material, Pair<List<Material>, Integer>> cachedRecipes;

        Cache(Player player) {
            this.player = player;
            this.inventory = player.getInventory();
            this.materialStack = new Stack<>();
            this.changeStack = new Stack<>();
            this.cachedSlots = new HashMap<>();
            this.cachedRecipes = new HashMap<>();
        }

        void storeUndo(Material material, int amount) {
            if (!changeStack.isEmpty()) {
                HashMap<Material, Integer> changes = changeStack.peek();
                changes.put(material, changes.getOrDefault(material, 0) + amount);
            }
        }

        void restoreInventory() {
            HashMap<Material, Integer> changes = changeStack.peek();
            for (Map.Entry<Material, Integer> undo : changes.entrySet()) {
                Material material = undo.getKey();
                int amount = undo.getValue();
                if (amount > 0)
                    give(undo.getKey(), amount, false);
                else {
                    int index;
                    do {
                        Pair<ItemStack, Integer> slot = nextMatching(material);
                        index = slot.getSecond();
                        ItemStack stack = slot.getFirst();
                        int change = Math.max(amount, -stack.getAmount());
                        stack.setAmount(stack.getAmount() + change);
                        amount -= change;

                        if (stack.getAmount() <= 0)
                            inventory.remove(stack);
                    } while (index > -1 && amount < 0);
                }
            }

            changes.clear();
        }

        void pushStack(Material material) {
            materialStack.push(material);
            changeStack.push(new HashMap<>());
        }

        void popStack() {
            materialStack.pop();
            HashMap<Material, Integer> newReversions = changeStack.pop();
            if (!changeStack.isEmpty()) {
                HashMap<Material, Integer> reversions = changeStack.peek();
                for (Map.Entry<Material, Integer> reversion : newReversions.entrySet()) {
                    if (reversions.containsKey(reversion.getKey()))
                        reversions.put(reversion.getKey(), reversions.get(reversion.getKey()) + reversion.getValue());
                    else
                        reversions.put(reversion.getKey(), reversion.getValue());
                }
            }
        }

        void give(Material material, int amount, boolean sendMessages) {
            if (amount > 0)
                storeUndo(material, -amount);

            while (amount > 0) {
                int stackSize = Math.min(amount, material.getMaxStackSize());
                Pair<ItemStack, Integer> slot = nextAddable(material);
                int index = slot.getSecond();

                if (index == -1) {
                    player.getWorld().dropItem(player.getLocation(), new ItemStack(material, stackSize));
                    if (sendMessages) {
                        player.sendMessage(ChatColor.RED + "Your inventory was full, so you dropped the item.");
                        sendMessages = false;
                    }
                    amount -= stackSize;
                } else {
                    ItemStack addTo = slot.getFirst();
                    if (addTo == null) {
                        inventory.setItem(index, new ItemStack(material, stackSize));
                        amount -= stackSize;
                    } else {
                        int currentAmount = addTo.getAmount();
                        int add = Math.min(material.getMaxStackSize() - currentAmount, amount);
                        addTo.setAmount(currentAmount + add);
                        amount -= add;
                    }
                }
            }
        }

        Pair<ItemStack, Integer> nextAddable(Material material) {
            Integer slot = cachedSlots.get(material);
            if (slot == null)
                slot = 0;

            int size = inventory.getSize();
            ItemStack stack;

            for (int i = 0; i < size; ++i) {
                int index = (slot + i) % size;
                stack = inventory.getItem(index);
                if (stack == null || (stack.getType() == material && stack.getAmount() < material.getMaxStackSize()) ||
                        stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                    cachedSlots.put(material, index);
                    return new Pair<>(stack, index);
                }
            }

            cachedSlots.remove(material);
            return new Pair<>(null, -1);
        }

        Pair<ItemStack, Integer> nextMatching(Material material) {
            Integer slot = cachedSlots.get(material);
            if (slot == null)
                slot = 0;

            int size = inventory.getSize();
            ItemStack stack;

            for (int i = 0; i < size; ++i) {
                int index = (slot + i) % size;
                stack = inventory.getItem(index);
                if (stack != null && stack.getType() == material) {
                    cachedSlots.put(material, index);
                    return new Pair<>(stack, index);
                }
            }

            cachedSlots.remove(material);
            return new Pair<>(null, -1);
        }
    }
}
