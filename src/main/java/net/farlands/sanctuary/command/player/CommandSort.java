package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Stream;

public class CommandSort extends PlayerCommand {

    public CommandSort() {
        super(
            CommandData.withRank(
                "sort",
                "Sort items in an inventory.",
                "/sort [container|echest|inventory] [quantity|alphabetical]",
                Rank.ESQUIRE
            )
        );
    }

    @Override
    public boolean execute(Player player, String[] args) {
        Inventory inventory;

        Sort sort = args.length == 2
            ? Utils.valueOfFormattedName(args[1], Sort.class)
            : Sort.QUANTITY;

        Block containerBlock = null;
        if (
           args.length > 0
           && args[0].equalsIgnoreCase("container")
           && (containerBlock = player.getTargetBlockExact(5)) != null
           && player.getTargetBlockExact(5)
                .getState()
                .getType() == Material.ENDER_CHEST
        ){
            args[0] = "echest";
        }

        int start = 0;
        if (args.length == 0 || args[0].equalsIgnoreCase("inventory")) {
            inventory = player.getInventory();
            start = 9; // skip hotbar;
        } else {
            switch (args[0].toLowerCase()) {
                case "container": {
                    if (containerBlock == null)
                        containerBlock = player.getTargetBlockExact(5);

                    // If the player is targeting a wall sign, get the container behind it.
                    if (containerBlock != null && containerBlock.getBlockData() instanceof WallSign s) {
                        BlockFace facing = s.getFacing();
                        containerBlock = containerBlock.getRelative(facing.getOppositeFace());
                    }

                    if (containerBlock == null || !(containerBlock.getState() instanceof Container container)) {
                        return error(player, "Target block must be a container");
                    }

                    FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(containerBlock.getLocation());
                    if (!(flags == null || flags.isEffectiveOwner(player))) {
                        if (!flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(player, TrustLevel.CONTAINER, flags)) {
                            return error(player, "This belongs to {}.", flags.getOwnerName());
                        }
                    }

                    inventory = container.getInventory();
                } break;
                case "echest": {
                    inventory = player.getEnderChest();
                } break;
                default:
                    return error(player, "Invalid target '{}'", args[0]);
            }; 
        }

        ItemStack[] items = inventory.getStorageContents();
        Map<ItemStack, Integer> counts = new HashMap<>();

        int startCount = 0;
        for (int i = start; i < items.length; ++i) {
            ItemStack item = items[i];
            if (item == null) continue;
            counts.compute(item.asOne(), (k, v) -> (v == null ? 0 : v) + item.getAmount());
            startCount += item.getAmount();
        }

        // sort by quantity
        var stream = counts.entrySet()
            .stream()
            .sorted(switch (sort) {
                case QUANTITY -> Comparator.comparingInt(e -> -e.getValue());
                case ALPHABETICAL -> Comparator.comparing(e -> e.getKey().getType().toString());
            })
            .toList();

        int i = start;
        for (Map.Entry<ItemStack, Integer> e : stream) {
            int n = e.getValue();
            ItemStack item = e.getKey();
            while (n > 0) {
                if (n > item.getMaxStackSize()) {
                    items[i++] = item.asQuantity(item.getMaxStackSize());
                    n -= item.getMaxStackSize();
                } else {
                    items[i++] = item.asQuantity(n);
                    n = 0;
                }
            }
        }
        Arrays.fill(items, i, items.length, null);
        
        int endCount = Arrays.stream(items)
            .skip(start)
            .mapToInt(item -> item == null ? 0 : item.getAmount())
            .sum();

        // sanity check
        if (endCount != startCount) {
            FarLands.getDebugger().echo("Invalid end count when sorting items.  start = " + startCount + ", end = " + endCount);
            return error(player, "An error occured while sorting your items.");
        }

        inventory.setStorageContents(items);
        return success(player, "Inventory sorted!");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return switch (args.length) {
            case 1 -> TabCompleterBase.filterStartingWith(args[0], Stream.of("container", "echest", "inventory"));
            case 2 -> TabCompleterBase.filterStartingWith(args[1], Stream.of("quantity", "alphabetical"));
            default -> Collections.emptyList();
        };
    }

    enum Sort {
        ALPHABETICAL,
        QUANTITY,
        ;
    }

}
