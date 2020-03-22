package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Pair;

import org.bukkit.Material;
import static org.bukkit.Material.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CommandStack extends PlayerCommand {

    /**
     * Items that do not unstack correctly when used in stacked format
     */
    private final static List<Material> UNSTACKABLES = Arrays.asList(
            MUSHROOM_STEW, RABBIT_STEW, BEETROOT_SOUP, LAVA_BUCKET, WATER_BUCKET,
            PUFFERFISH_BUCKET, COD_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET
    );

    /**
     * All items that group together on being stacked
     * 0 index containing the most stacked material type of the given item
     */
    private static final List<Material[]> SIMILAR = Arrays.asList(
            new Material[] {DRIED_KELP_BLOCK, DRIED_KELP},        new Material[] {HAY_BLOCK, WHEAT},
            new Material[] {MELON, MELON_SLICE},                  new Material[] {SLIME_BLOCK, SLIME_BALL},
            new Material[] {BONE_BLOCK, BONE_MEAL},               new Material[] {COAL_BLOCK, COAL},
            new Material[] {DIAMOND_BLOCK, DIAMOND},              new Material[] {EMERALD_BLOCK, EMERALD},
            new Material[] {IRON_BLOCK, IRON_INGOT, IRON_NUGGET}, new Material[] {GOLD_BLOCK, GOLD_INGOT, GOLD_NUGGET},
            new Material[] {REDSTONE_BLOCK, REDSTONE},            new Material[] {LAPIS_BLOCK, LAPIS_LAZULI}
    );

    public CommandStack() {
        super(Rank.ESQUIRE, "Stack all items of a similar type in your inventory.", "/stack", "stack", "condense");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        ItemStack[] inventory = sender.getInventory().getStorageContents(); // copy of player inventory
        Material rKey; // key for stacking material
        Map<Material, List<Integer>> returns = new HashMap<>(); // list of inventory indexes items can be returned to
        Pair<ItemStack, Integer> item1, // the item we're looking at to stack
                                 item2; // the item we check to see if it stacks with item1
        List<ItemStack> items; // the item stacks returned after stacking item1

        // warning lists so they can be displayed in a compact manor at the end
        List<Material> warningsUnstack = new ArrayList<>(),
                       warningsShulker = new ArrayList<>();

        for (int i = 0, j; i < inventory.length; ++i) {
            if (inventory[i] == null) // Skip empty slots
                continue;
            rKey = returnsKey(inventory[i].getType());
            if (!inventory[i].hasItemMeta() && returns.containsKey(rKey)) // Skip already stacked items
                continue;

            // Unstack similar items
            item1 = unstack(inventory[i].clone());

            returns.put(rKey, new ArrayList<>());
            inventory[i] = null;

            for (j = i; ++j < inventory.length; ) {
                if (inventory[j] == null) // Skip empty slots
                    continue;

                item2 = unstack(inventory[j].clone());
                if (item1.getFirst().isSimilar(item2.getFirst())) {
                    item1.setSecond(item1.getSecond() + item2.getSecond());
                    returns.get(rKey).add(j);
                    inventory[j] = null;
                }
            }

            // Stack items
            items = stack(item1);
            if (items.size() <= 0)
                continue;

            // add warnings
            j = items.size() - 1;
            if (UNSTACKABLES.contains(items.get(j).getType()) && items.get(j).getAmount() > 1 &&
                    !warningsUnstack.contains(items.get(j).getType()))
                warningsUnstack.add(items.get(j).getType());
            if (items.get(j).getAmount() > items.get(j).getMaxStackSize() &&
                    !warningsShulker.contains(items.get(j).getType()))
                warningsShulker.add(items.get(j).getType());

            // put items back
            inventory[i] = items.get(j).clone();
            for (; --j >= 0; ) {
                if (firstNull(inventory) < 0) { // drop everything if the inventory is "full"
                    for (; j >= 0; --j)
                        sender.getWorld().dropItem(sender.getLocation(), items.get(j).clone());
                    break;
                }

                if (returns.get(rKey).size() > 0) { // if there's a slot to return the item to put it there
                    inventory[returns.get(rKey).get(0)] = items.get(j).clone();
                    returns.get(rKey).remove(0);
                } else
                    inventory[firstNull(inventory)] = items.get(j).clone();
            }
        }
        sender.getInventory().setStorageContents(inventory);

        // send warnings
        final StringBuilder warning = new StringBuilder();
        if (!warningsUnstack.isEmpty()) {
            warning.append("&(red)The following $(hover,&(gray)");
            warningsUnstack.forEach(item -> warning.append(item.name()).append(" "));
            warning.append(",&(bold)items) should be $(hover," +
                    "&(gray)These items are prone to deletion on use when stacked," +
                    "&(bold)unstacked before use).");
            sendFormatted(sender, warning.toString());
            warning.setLength(0);
        }
        if (!warningsShulker.isEmpty()) {
            warning.append("&(red)The following $(hover,&(gray)");
            warningsShulker.forEach(item -> warning.append(item.name()).append(" "));
            warning.append(",&(bold)items) are $(hover," +
                    "&(gray)These items will unstack when stored in a shulker box which overwrites and deletes items," +
                    "&(bold)not safe) to store in a shulker box.");
            sendFormatted(sender, warning.toString());
            // warning.setLength(0); // uncomment for further warnings
        }

        return true;
    }

    /**
     * Convert similar stacking materials into its largest stacking material
     * @param material the material to convert
     * @return the material key
     */
    private static Material returnsKey(Material material) {
        for (Material[] materials : SIMILAR)
            for (Material loopMaterial : materials)
                if (loopMaterial == material)
                    return materials[0];
        return material;
    }

    /**
     * Find the first null (empty) space in the inventory
     * @param inventory the inventory to check
     * @return index of first empty space, -1 if none.
     */
    private static int firstNull(ItemStack[] inventory) {
        for (int i = 0; i < inventory.length; ++i)
            if (inventory[i] == null)
                return i;
        return -1;
    }

    /**
     * Convert an item into it's smallest stackable form
     * Returned as a pair of item, int to prevent overflow on item#amount byte value
     * @param item the item to unstack
     * @return the item object with amount attached
     */
    private static Pair<ItemStack, Integer> unstack(ItemStack item) {
        int amount = item.getAmount();
        for (Material[] materials : SIMILAR)
            for (int j = 0; j < materials.length - 1; ++j)
                if (item.getType().equals(materials[j])) {
                    item.setType(materials[j + 1]);
                    amount *= 9;
                }
        return new Pair<>(item, amount);
    }

    /**
     * Stack up an item into a list of ItemStack that is compatible with the vanilla inventory
     * @param item the item and attached amount to stack
     * @return a list of items that correlate to the correct stacked amounts
     */
    private static List<ItemStack> stack(Pair<ItemStack, Integer> item) {
        List<ItemStack> items = new ArrayList<>();
        for (Material[] materials : SIMILAR) {
            for (int j = materials.length; --j > 0; ) {
                if (item.getFirst().getType().equals(materials[j])) {
                    if (item.getSecond() % 9 != 0) {
                        item.getFirst().setAmount(item.getSecond() % 9);
                        items.add(item.getFirst().clone());
                    }
                    item.getFirst().setType(materials[j - 1]);
                    item.setSecond(item.getSecond() / 9);
                }
            }
        }

        if ((item.getSecond() & 63) != 0) { // x % 64
            item.getFirst().setAmount(item.getSecond() & 63); // x % 64
            items.add(item.getFirst().clone());
        }
        item.getFirst().setAmount(64);
        for (int i = 0, e = item.getSecond() >> 6; ++i <= e; ) // x / 64
            items.add(item.getFirst().clone());

        return items;
    }
}
