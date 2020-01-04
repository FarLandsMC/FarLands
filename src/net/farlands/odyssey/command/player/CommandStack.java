package net.farlands.odyssey.command.player;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Pair;

import static org.bukkit.Material.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandStack extends PlayerCommand {

    private final static List<Material> UNSTACKABLES = Arrays.asList(MUSHROOM_STEW, RABBIT_STEW, BEETROOT_SOUP,
            WATER_BUCKET, PUFFERFISH_BUCKET, COD_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET, LAVA_BUCKET);
    private final static List<Material> ENCHANTABLES = new ArrayList<>();

    private static final List<Material[]> SIMILAR = Arrays.asList(
            new Material[]{DRIED_KELP_BLOCK, DRIED_KELP}, new Material[]{HAY_BLOCK, WHEAT},
            new Material[]{MELON, MELON_SLICE}, new Material[]{SLIME_BLOCK, SLIME_BALL},
            new Material[]{BONE_BLOCK, BONE_MEAL}, new Material[]{COAL_BLOCK, COAL},
            new Material[]{DIAMOND_BLOCK, DIAMOND}, new Material[]{EMERALD_BLOCK, EMERALD},
            new Material[]{IRON_BLOCK, IRON_INGOT, IRON_NUGGET}, new Material[]{GOLD_BLOCK, GOLD_INGOT, GOLD_NUGGET},
            new Material[]{REDSTONE_BLOCK, REDSTONE}, new Material[]{LAPIS_BLOCK, LAPIS_LAZULI}
    );

    static {
        ENCHANTABLES.addAll(Arrays.stream(Material.values()).filter(material -> !material.name().startsWith("LEGACY_") &&
                Stream.of("_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS", "_SWORD", "_AXE", "_PICKAXE", "_SHOVEL", "_HOE")
                        .anyMatch(material.name()::endsWith)).collect(Collectors.toList()));
        ENCHANTABLES.addAll(Arrays.asList(ELYTRA, TURTLE_HELMET, BOW, CROSSBOW, TRIDENT, ENCHANTED_BOOK,
                FISHING_ROD, SHEARS, FLINT_AND_STEEL, CARROT_ON_A_STICK));
    }

    public CommandStack() {
        super(Rank.ESQUIRE, "Stack all items of a similar type in your inventory.", "/stack", "stack", "condense");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        Map<Material, List<Integer>> returns = new HashMap<>();
        ItemStack[] inv = sender.getInventory().getStorageContents();
        for (int i = 0; i < inv.length; ++i) {
            if (inv[i] == null) // Skip empty slots
                continue;
            Material rKey = returnsKey(inv[i].getType());
            if (returns.containsKey(rKey) && !ENCHANTABLES.contains(rKey)) // Skip already stacked items
                continue;

            // Unstack similar items
            Pair<ItemStack, Integer> item1 = unstack(inv[i].clone());

            returns.put(rKey, new ArrayList<>());
            inv[i] = null;

            for (int j = i; ++j < inv.length; ) {
                if (inv[j] == null) // Skip empty slots
                    continue;

                Pair<ItemStack, Integer> item2 = unstack(inv[j].clone());
                if (item1.getFirst().isSimilar(item2.getFirst())) {
                    item1.setSecond(item1.getSecond() + item2.getSecond());
                    List<Integer> temp = returns.get(rKey);
                    temp.add(j);
                    returns.put(rKey, temp);
                    inv[j] = null;
                }
            }

            // Stack items
            List<ItemStack> items = stack(item1);
            if (items.size() <= 0)
                continue;
            // warn
            if (UNSTACKABLES.contains(items.get(items.size() - 1).getType()) && items.get(items.size() - 1).getAmount() > 1)
                sender.sendMessage(ChatColor.RED + "Item [" + items.get(items.size() - 1).getType().toString() +
                        "] should be unstacked before use.");
            if (items.get(items.size() - 1).getAmount() > items.get(items.size() - 1).getMaxStackSize())
                sender.sendMessage(ChatColor.RED + "Item [" + items.get(items.size() - 1).getType().toString() +
                        "] is not safe to store in a shulker box");
            // put items back
            inv[i] = items.get(items.size() - 1).clone();
            for (int j = items.size() - 1; --j >= 0; ) {
                if (firstNull(inv) < 0) {
                    sender.getWorld().dropItem(sender.getLocation(), items.get(j).clone());
                    continue;
                }

                if (returns.containsKey(rKey)) {
                    List<Integer> returnsList = returns.get(rKey);
                    if (returnsList.size() <= 0) {
                        returns.remove(rKey);
                        inv[firstNull(inv)] = items.get(j).clone();
                        continue;
                    }
                    inv[returnsList.get(0)] = items.get(j).clone();
                    returns.get(rKey).remove(0);
                } else
                    inv[firstNull(inv)] = items.get(j).clone();
            }
        }
        sender.getInventory().setStorageContents(inv);
        return true;
    }

    private static Material returnsKey(Material material) {
        for (Material[] materials : SIMILAR) {
            for (Material mat : materials) {
                if (mat == material)
                    return materials[0];
            }
        }
        return material;
    }

    private static int firstNull(ItemStack[] items) {
        for (int i = 0; i < items.length; ++i) {
            if (items[i] == null)
                return i;
        }
        return -1;
    }

    private static Pair<ItemStack, Integer> unstack(ItemStack item) {
        int amount = item.getAmount();
        for (Material[] materials : SIMILAR) {
            for (int j = 0; j < materials.length - 1; ++j) {
                if (item.getType().equals(materials[j])) {
                    item.setType(materials[j + 1]);
                    amount *= 9;
                }
            }
        }
        return new Pair<>(item, amount);
    }

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
        for (int i = -1, e = item.getSecond() / 64; ++i < e; )
            items.add(item.getFirst().clone());

        return items;
    }
}
