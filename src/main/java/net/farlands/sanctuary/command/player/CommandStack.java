package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.util.Materials;
import com.kicas.rp.util.Pair;
import com.kicas.rp.util.ReflectionHelper;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.LocationWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;
import java.util.stream.Stream;

import static org.bukkit.Material.*;

public class CommandStack extends PlayerCommand {

    /**
     * Items that do not unstack correctly when used in stacked format
     */
    private final static List<Material> UNSTACKABLES = Arrays.asList(
            MUSHROOM_STEW, RABBIT_STEW, BEETROOT_SOUP, LAVA_BUCKET, WATER_BUCKET, POWDER_SNOW_BUCKET,
            PUFFERFISH_BUCKET, COD_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET,
            ENCHANTED_BOOK, POTION
    );

    /**
     * All items that group together on being stacked
     * 0 index containing the most stacked material type of the given item
     */
    private static final List<Material[]> SIMILAR = Arrays.asList(
            new Material[] { BAMBOO_BLOCK,     BAMBOO                       },
            new Material[] { DRIED_KELP_BLOCK, DRIED_KELP                   },
            new Material[] { BONE_BLOCK,       BONE_MEAL                    },
            new Material[] { DIAMOND_BLOCK,    DIAMOND                      },
            new Material[] { IRON_BLOCK,       IRON_INGOT,      IRON_NUGGET },
            new Material[] { REDSTONE_BLOCK,   REDSTONE                     },
            new Material[] { NETHERITE_BLOCK,  NETHERITE_INGOT,             },
            new Material[] { HAY_BLOCK,        WHEAT                        },
            new Material[] { SLIME_BLOCK,      SLIME_BALL                   },
            new Material[] { COAL_BLOCK,       COAL                         },
            new Material[] { EMERALD_BLOCK,    EMERALD                      },
            new Material[] { GOLD_BLOCK,       GOLD_INGOT,      GOLD_NUGGET },
            new Material[] { LAPIS_BLOCK,      LAPIS_LAZULI                 },
            new Material[] { COPPER_BLOCK,     COPPER_INGOT                 },
            new Material[] { RAW_COPPER_BLOCK, RAW_COPPER                   },
            new Material[] { RAW_IRON_BLOCK,   RAW_IRON                     },
            new Material[] { RAW_GOLD_BLOCK,   RAW_GOLD                     }
    );

    private static final List<Material> ACCEPTED_CONTAINERS = new ArrayList<>();
    static {
        ACCEPTED_CONTAINERS.addAll(Arrays.asList(CHEST, TRAPPED_CHEST, BARREL));
        ACCEPTED_CONTAINERS.addAll(Materials.materialsEndingWith("SHULKER_BOX"));
    }

    public CommandStack() {
        super(
            CommandData.withRank(
                "stack",
                "Stack all items of a similar type in your inventory.",
                "/stack [container|echest|hand]",
                Rank.ESQUIRE
            ).aliases("condense")
        );
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // warning lists so they can be displayed in a compact manner at the end
        List<Material> warningsUnstack = new ArrayList<>();

        if (args.length <= 0) {
            player.getInventory().setStorageContents(
                    stack(player, player.getInventory().getStorageContents(), player.getLocation(), warningsUnstack)
            );
            sendWarnings(player, warningsUnstack);
            return true;
        }

        Block block = null;

        if(args[0].equalsIgnoreCase("container") &&
           (block = player.getTargetBlockExact(5)) != null &&
            player.getTargetBlockExact(5)
                .getState()
                .getType() == Material.ENDER_CHEST
        ){
            args[0] = "echest";
        }

        switch (args[0].toLowerCase()) {
            case "container": {
                // If the player is targeting a wall sign, get the container behind it.
                if (block != null && block.getBlockData() instanceof WallSign s) {
                    BlockFace facing = s.getFacing();
                    block = block.getRelative(facing.getOppositeFace());
                }

                BlockEntity tileEntity;
                // tileEntity = ((CraftWorld) player.getWorld()).getHandle().getBlockEntity().getBlockEntity(...)
                if (block == null || (tileEntity =
                    ((ServerLevel) ReflectionHelper.invoke("getHandle", FLUtils.getCraftBukkitClass("CraftWorld"), player.getWorld()))
                        .getBlockEntity(new LocationWrapper(block.getLocation()).asBlockPos(), true)) == null ||
                    !ACCEPTED_CONTAINERS.contains(block.getType())) {
                    return error(player, "Target block must be a chest or barrel");
                }

                FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(block.getLocation());
                if (!(flags == null || flags.isEffectiveOwner(player))) {
                    if (!flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(player, TrustLevel.CONTAINER, flags)) {
                        return error(player, "This belongs to {}.", flags.getOwnerName());
                    }
                }

                tileEntity.getOwner().getInventory().setStorageContents(
                        stack(player, tileEntity.getOwner().getInventory().getStorageContents(),
                                block.getLocation().add(0.5, 1.5, 0.5), warningsUnstack)
                );
                if (sendWarnings(player, warningsUnstack))
                    success(player, "Container contents stacked!");
                return true;
            }
            case "echest": {
                player.getEnderChest().setStorageContents(
                        stack(player, player.getEnderChest().getStorageContents(), player.getLocation(), warningsUnstack)
                );

                if (sendWarnings(player, warningsUnstack))
                    success(player, "Ender chest contents stacked!");
                return true;
            }
            case "hand": {
                ItemStack[] storageContents = player.getInventory().getStorageContents();
                stackType(player.getInventory().getHeldItemSlot(), storageContents, player.getLocation(), new ArrayList<>(), new HashMap<>());
                player.getInventory().setStorageContents(storageContents);

                sendWarnings(player, warningsUnstack);
                return true;
            }
            case "hotbar": {
                ItemStack[] storageContents = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    storageContents[i] = player.getInventory().getItem(i);
                }
                ItemStack[] returnedContents = stack(player, storageContents, player.getLocation(), new ArrayList<>());
                for (int i = 0; i < returnedContents.length; i++) {
                    player.getInventory().setItem(i, returnedContents[i]);
                }

                sendWarnings(player, warningsUnstack);
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Stream.of("container", "echest", "hand", "hotbar"))
                : Collections.emptyList();
    }

    private ItemStack[] stack(Player player, ItemStack[] storageContents, Location dropLocation,
                              List<Material> warningsUnstack) {
        Map<Material, List<Integer>> returns = new HashMap<>(); // list of inventory indexes items can be returned to

        boolean warnFullInventory = false; // if items are caused to drop due to full inventory

        if (dropLocation.getWorld() == null)
            dropLocation.setWorld(player.getWorld());

        int i;
        for (i = storageContents.length; --i >= 0; ) {
            if (storageContents[i] == null) // Skip empty slots
                continue;

            // if it's a shulker, stack the contents of the shulker
            if (storageContents[i].getType().name().endsWith("SHULKER_BOX")) {
                BlockStateMeta blockStateMeta = (BlockStateMeta) storageContents[i].getItemMeta();
                ShulkerBox blockState = (ShulkerBox) blockStateMeta.getBlockState();
                blockState.getInventory().setStorageContents(
                        stack(player, blockState.getInventory().getStorageContents(), dropLocation, warningsUnstack)
                );
                blockStateMeta.setBlockState(blockState);
                storageContents[i].setItemMeta(blockStateMeta);
            }
        }

        for (i = 0; i < storageContents.length; ++i) {


            if (stackType(i, storageContents, dropLocation, warningsUnstack, returns))
                warnFullInventory = true;
        }

        if (warnFullInventory)
            error(player, "Some items were dropped as the inventory was full");

        return storageContents;
    }

    private boolean stackType(int i, ItemStack[] storageContents, Location dropLocation, List<Material> warningsUnstack, Map<Material, List<Integer>> returns) {
        if (storageContents[i] == null) // Skip empty slots
            return false;

        Material rKey = returnsKey(storageContents[i].getType()); // key for stacking material
        if (returns.containsKey(rKey)) // Skip already stacked items
            return false;

        Pair<ItemStack, Integer> item1, // the item we're looking at to stack
                                 item2; // the item we check to see if it stacks with item1
        List<ItemStack> items; // the item stacks returned after stacking item1

        boolean warnFullInventory = false; // if items are caused to drop due to full inventory
        int j;


        // Unstack similar items
        item1 = unstack(storageContents[i].clone());

        item1.getFirst().editMeta(im -> {
            im.setMaxStackSize(64);
        });

        returns.put(rKey, new ArrayList<>());
        storageContents[i] = null;

        for (j = i; ++j < storageContents.length; ) {
            if (storageContents[j] == null) // Skip empty slots
                continue;

            item2 = unstack(storageContents[j].clone());
            item2.getFirst().editMeta(im -> {
                im.setMaxStackSize(64);
            });
            if (item1.getFirst().isSimilar(item2.getFirst())) {
                item1.setSecond(item1.getSecond() + item2.getSecond());
                returns.get(rKey).add(j);
                storageContents[j] = null;
            }
        }

        // Stack items
        items = stack(item1);
        if (items.size() <= 0)
            return false;

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

        return warnFullInventory;
    }

    private boolean sendWarnings(Player player, List<Material> warningsUnstack) {

        if (!warningsUnstack.isEmpty()) {
            error(
                player,
                "The following {} should be {}.",
                ComponentUtils.hover(
                    Component.text("items").style(Style.style(NamedTextColor.RED, TextDecoration.BOLD)),
                    Component.join(
                            JoinConfiguration.separator(Component.space()),
                            warningsUnstack.stream()
                                .map(e -> Component.translatable(e.translationKey()))
                                .toList()
                        )
                        .color(NamedTextColor.GRAY)
                ),
                ComponentUtils.hover(
                    Component.text("unstacked before use").style(Style.style(NamedTextColor.RED, TextDecoration.BOLD)),
                    ComponentColor.gray("These items are prone to deletion on use when stacked")
                )
            );
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

        if (item.getSecond() % 64 != 0) {
            item.getFirst().setAmount(item.getSecond() % 64);
            items.add(item.getFirst().clone());
        }
        item.getFirst().setAmount(64);
        for (int i = 0, e = item.getSecond() / 64; ++i <= e; )
            items.add(item.getFirst().clone());

        return items;
    }

}
