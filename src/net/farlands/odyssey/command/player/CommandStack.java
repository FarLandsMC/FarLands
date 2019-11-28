package net.farlands.odyssey.command.player;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Pair;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandStack extends PlayerCommand {
    
    private final static List<Material> UNSTACKABLES = Arrays.asList(Material.MUSHROOM_STEW, Material.RABBIT_STEW,
            Material.BEETROOT_SOUP, Material.WATER_BUCKET, Material.PUFFERFISH_BUCKET, Material.COD_BUCKET,
            Material.SALMON_BUCKET, Material.TROPICAL_FISH_BUCKET, Material.LAVA_BUCKET);
    private final static Material[] STACKED = {Material.DRIED_KELP_BLOCK, Material.HAY_BLOCK, Material.MELON, Material.SLIME_BLOCK,
            Material.COAL_BLOCK, Material.LAPIS_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
            Material.IRON_BLOCK, Material.IRON_INGOT, Material.GOLD_BLOCK, Material.GOLD_INGOT, Material.REDSTONE_BLOCK,
            Material.BONE_BLOCK};
    private final static Material[] UNSTACKED = {Material.DRIED_KELP, Material.WHEAT, Material.MELON_SLICE, Material.SLIME_BALL,
            Material.COAL, Material.LAPIS_LAZULI, Material.DIAMOND, Material.EMERALD,
            Material.IRON_INGOT, Material.IRON_NUGGET, Material.GOLD_INGOT, Material.GOLD_NUGGET, Material.REDSTONE,
            Material.BONE_MEAL};
    
    public CommandStack() {
        super(Rank.ESQUIRE, "Stack all items of a similar type in your inventory.", "/stack", "stack", "condense");
    }
    
    @Override
    public boolean execute(Player sender, String[] args) {
        int i, empty = -1, next = 0;
        ItemStack[] inv = sender.getInventory().getStorageContents();
        do {
            i = next;
            if (inv[i] == null) {
                if (++next >= inv.length)
                    break;
                continue;
            }
            Pair<ItemStack, Integer> item1 = unstack(inv[i].clone());
            inv[i] = null;
            // find all similar items and unstack them
            for (int j = i; ++j < inv.length; ) {
                if (inv[j] == null) {
                    if (empty < 0)
                        empty = j;
                    continue;
                }
                Pair<ItemStack, Integer> item2 = unstack(inv[j].clone());
                if (item1.getFirst().isSimilar(item2.getFirst())) {
                    item1.setSecond(item1.getSecond() + item2.getSecond());
                    inv[j] = null;
                    if (empty < 0)
                        empty = j;
                } else if (i == next)
                    next = j;
            }
            // stack it all back up
            List<ItemStack> items = stack(item1);
            if (items.size() <= 0)
                continue;
            // warn
            if (UNSTACKABLES.contains(items.get(items.size() - 1).getType()) && items.get(items.size() - 1).getAmount() > 1)
                sender.sendMessage(ChatColor.RED + "Item [" + items.get(items.size() - 1).getType().toString() +
                        "] should be unstacked before use.");
            if(items.get(items.size() - 1).getMaxStackSize() == 1 && items.get(items.size() - 1).getAmount() > 1)
                sender.sendMessage(ChatColor.RED + "Item [" + items.get(items.size() - 1).getType().toString() +
                        "] is not safe to store in a shulker box");
            // put items back
            inv[i] = items.get(items.size() - 1).clone();
            for (int j = items.size() - 1; --j >= 0; ) {
                if (empty < 0) {
                    sender.getWorld().dropItem(sender.getLocation(), items.get(j).clone());
                    continue;
                }
                if (inv[empty] != null) {
                    ++j;
                    if (++empty >= 36)
                        empty = -1;
                    continue;
                }
                inv[empty] = items.get(j).clone();
            }
        } while (i != next);
        sender.getInventory().setStorageContents(inv);
        return true;
    }
    
    private static Pair<ItemStack, Integer> unstack(ItemStack item) {
        int amount = item.getAmount();
        for (int i = -1; ++i < STACKED.length; ) {
            if (item.getType().equals(STACKED[i])) {
                item.setType(UNSTACKED[i]);
                amount *= 9;
            }
        }
        return new Pair<>(item, amount);
    }
    private static List<ItemStack> stack(Pair<ItemStack, Integer> item) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = UNSTACKED.length; --i >= 0; ) {
            if (item.getFirst().getType().equals(UNSTACKED[i])) {
                if (item.getSecond() % 9 != 0) {
                    item.getFirst().setAmount(item.getSecond() % 9);
                    items.add(item.getFirst().clone());
                }
                item.getFirst().setType(STACKED[i]);
                item.setSecond(item.getSecond() / 9);
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
