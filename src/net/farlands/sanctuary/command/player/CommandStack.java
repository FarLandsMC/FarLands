package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.util.Materials;

import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.LocationWrapper;
import net.farlands.sanctuary.util.Pair;

import net.minecraft.server.v1_16_R1.TileEntity;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import static org.bukkit.Material.*;

import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;
import java.util.stream.Stream;

public class CommandStack extends PlayerCommand {

    /**
     * Items that do not unstack correctly when used in stacked format
     */
    private final static List<Material> UNSTACKABLES = Arrays.asList(
            MUSHROOM_STEW, RABBIT_STEW, BEETROOT_SOUP, LAVA_BUCKET, WATER_BUCKET,
            PUFFERFISH_BUCKET, COD_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET,
            ENCHANTED_BOOK, POTION
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
            new Material[] {REDSTONE_BLOCK, REDSTONE},            new Material[] {LAPIS_BLOCK, LAPIS_LAZULI},
            new Material[] {NETHERITE_BLOCK, NETHERITE_INGOT}
    );

    private static final List<Material> ACCEPTED_CONTAINERS = new ArrayList<>();
    static {
        ACCEPTED_CONTAINERS.addAll(Arrays.asList(CHEST, TRAPPED_CHEST, BARREL));
        ACCEPTED_CONTAINERS.addAll(Materials.materialsEndingWith("SHULKER_BOX"));
    }

    public CommandStack() {
        super(Rank.ESQUIRE, "Stack all items of a similar type in your inventory.", "/stack [container|echest]",
                "stack", "condense");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // warning lists so they can be displayed in a compact manor at the end
        List<Material> warningsUnstack = new ArrayList<>();

        if (args.length == 0) {
            player.getInventory().setStorageContents(
                    stack(player, player.getInventory(), player.getLocation(), warningsUnstack)
            );
            sendWarnings(player, warningsUnstack);
            return true;
        }

        if (args[0].equalsIgnoreCase("container")) {
            Block block = player.getTargetBlockExact(5);
            TileEntity tileEntity;
            if (block == null || (tileEntity = ((CraftWorld) player.getWorld()).getHandle()
                    .getTileEntity(new LocationWrapper(block.getLocation()).asBlockPosition())) == null ||
                    !ACCEPTED_CONTAINERS.contains(block.getType())) {
                player.sendMessage(ChatColor.RED + "Target block must be a chest or barrel");
                return true;
            }

            FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(block.getLocation());
            if (!(flags == null || flags.isEffectiveOwner(player))) {
                if (!flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(player, TrustLevel.CONTAINER, flags)) {
                    player.sendMessage(ChatColor.RED + "This belongs to " + flags.getOwnerName() + ".");
                    return true;
                }
            }

            tileEntity.getOwner().getInventory().setStorageContents(
                    stack(player, tileEntity.getOwner().getInventory(), block.getLocation().add(0.5, 1.5, 0.5),
                            warningsUnstack)
            );
            if (sendWarnings(player, warningsUnstack))
                player.sendMessage(ChatColor.GREEN + "Container contents stacked!");
            return true;
        } else if (args[0].equalsIgnoreCase("echest")) {
            player.getEnderChest().setStorageContents(
                    stack(player, player.getEnderChest(), player.getLocation(), warningsUnstack)
            );

            if (sendWarnings(player, warningsUnstack))
                player.sendMessage(ChatColor.GREEN + "Ender chest contents stacked!");
            return true;
        }

        return false;
    }

    private ItemStack[] stack(Player player, Inventory inventory, Location dropLocation,
                              List<Material> warningsUnstack) {

        ItemStack[] storageContents = inventory.getStorageContents(); // copy of inventory

        Material rKey; // key for stacking material
        Map<Material, List<Integer>> returns = new HashMap<>(); // list of inventory indexes items can be returned to

        Pair<ItemStack, Integer> item1, // the item we're looking at to stack
                                 item2; // the item we check to see if it stacks with item1
        List<ItemStack> items; // the item stacks returned after stacking item1

        boolean warnFullInventory = false; // if items are caused to drop due to full inventory

        if (dropLocation.getWorld() == null)
            dropLocation.setWorld(player.getWorld());

        int i, j;
        for (i = storageContents.length; --i >= 0; ) {
            if (storageContents[i] == null) // Skip empty slots
                continue;

            // if it's a shulker, stack the contents of the shulker
            if (storageContents[i].getType().name().endsWith("SHULKER_BOX")) {
                BlockStateMeta blockStateMeta = (BlockStateMeta) storageContents[i].getItemMeta();
                ShulkerBox blockState = (ShulkerBox) blockStateMeta.getBlockState();
                blockState.getInventory().setStorageContents(
                        stack(player, blockState.getInventory(), dropLocation, warningsUnstack)
                );
                blockStateMeta.setBlockState(blockState);
                storageContents[i].setItemMeta(blockStateMeta);
            }
        }

        for (i = 0; i < storageContents.length; ++i) {
            if (storageContents[i] == null) // Skip empty slots
                continue;

            rKey = returnsKey(storageContents[i].getType());
            if (returns.containsKey(rKey)) // Skip already stacked items
                continue;

            // Unstack similar items
            item1 = unstack(storageContents[i].clone());

            returns.put(rKey, new ArrayList<>());
            storageContents[i] = null;

            for (j = i; ++j < storageContents.length; ) {
                if (storageContents[j] == null) // Skip empty slots
                    continue;

                item2 = unstack(storageContents[j].clone());
                if (item1.getFirst().isSimilar(item2.getFirst())) {
                    item1.setSecond(item1.getSecond() + item2.getSecond());
                    returns.get(rKey).add(j);
                    storageContents[j] = null;
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

            // put items back
            storageContents[i] = items.get(j).clone();
            for (; --j >= 0; ) {
                if (firstNull(storageContents) < 0) { // drop everything if the inventory is "full"
                    warnFullInventory = true;
                    for (; j >= 0; --j)
                        dropLocation.getWorld().dropItem(dropLocation, items.get(j).clone());
                    break;
                }

                if (returns.get(rKey).size() > 0) { // if there's a slot to return the item to put it there
                    storageContents[returns.get(rKey).get(0)] = items.get(j).clone();
                    returns.get(rKey).remove(0);
                } else
                    storageContents[firstNull(storageContents)] = items.get(j).clone();
            }

            // remove items with nbt so we can attempt to stack other items of the same type that may differ in nbt
            if (item1.getFirst().hasItemMeta())
                returns.remove(rKey);
        }

        if (warnFullInventory)
            sendFormatted(player, "&(red)Some items were dropped as the inventory was full");

        return storageContents;
    }

    private boolean sendWarnings(Player player, List<Material> warningsUnstack) {
        // send warnings
        final StringBuilder warning = new StringBuilder();

        if (!warningsUnstack.isEmpty()) {
            warning.append("&(red)The following $(hover,&(gray)");
            warningsUnstack.forEach(item -> warning.append(item.name()).append(" "));
            warning.delete(warning.length() - 1, warning.length());
            warning.append(",&(bold)items) should be $(hover," +
                    "&(gray)These items are prone to deletion on use when stacked," +
                    "&(bold)unstacked before use).");
            sendFormatted(player, warning.toString());
            warning.setLength(0);
            return false;
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

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Stream.of("container", "echest"))
                : Collections.emptyList();
    }
}
