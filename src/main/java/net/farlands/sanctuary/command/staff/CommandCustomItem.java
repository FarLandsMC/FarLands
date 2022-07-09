package net.farlands.sanctuary.command.staff;

import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.DataHandler;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.gui.VPRewardsGui;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CommandCustomItem extends PlayerCommand {

    private final Map<UUID, PendingConfirm> pendingConfirms;
    private final DataHandler dh;

    public CommandCustomItem() {
        super(Rank.BUILDER, "Manage custom items in the plugin", "/customitem <get|put|remove|confirm|cancel> <key>", "customitem", "customitems");
        this.pendingConfirms = new HashMap<>();
        this.dh = FarLands.getDataHandler();
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) return false;
        Action action = Utils.valueOfFormattedName(args[0], Action.class);
        if (action == null) return false;
        if (args.length < requiredArgsLength(action)) return false;

        switch (action) {
            case LIST -> {
                if (dh.getItems().isEmpty()) {
                    player.sendMessage(ComponentColor.red("There are no custom items saved."));
                    return true;
                }

                List<Component> items = dh.getItems()
                    .entrySet()
                    .stream()
                    .map(e -> ComponentColor.aqua(e.getKey())
                        .hoverEvent(e.getValue().asHoverEvent())
                    ).toList();

                Component component = Component
                    .text()
                    .content("The following keys are saved as custom items (hover to view the item):\n")
                    .color(NamedTextColor.GOLD)
                    .append(
                        Component.join(
                            JoinConfiguration.separators(
                                Component.text(", "),
                                Component.text(", and ")
                            ),
                            items
                        )
                    )
                    .build();

                player.sendMessage(component);
                return true;


            }
            case GET -> {
                ItemStack stack = dh.getItem(args[1]);
                if (stack == null) {
                    player.sendMessage(ComponentColor.red("No item found with the key: %s", args[1]));
                    return true;
                }
                FLUtils.giveItem(player, stack, true);
                return true;
            }
            case VP_REWARDS -> {
                var gui = new VPRewardsGui();
                gui.openGui(player);
                player.sendMessage(ComponentColor.gold(
                    """
                        Left Click: Increase Rarity
                        Right Click: Decrease Rarity
                        Shift + Left Click: Get Copy of Item
                        Shift + Right Click: Remove Item"""
                ));
                return true;

            }
            case CONFIRM -> {
                if (!pendingConfirms.containsKey(player.getUniqueId())) {
                    player.sendMessage(ComponentColor.red("No actions pending a confirmation."));
                    return true;
                }
                PendingConfirm pend = pendingConfirms.get(player.getUniqueId());
                handle(player, pend.command, pend.key, pend.item);
                pendingConfirms.remove(player.getUniqueId());
                Bukkit.getScheduler().cancelTask(pend.taskId);
                return true;
            }
            case CANCEL -> {
                if (!pendingConfirms.containsKey(player.getUniqueId())) {
                    player.sendMessage(ComponentColor.red("No actions pending a confirmation."));
                    return true;
                }

                PendingConfirm pend = pendingConfirms.get(player.getUniqueId());
                Bukkit.getScheduler().cancelTask(pend.taskId);
                pendingConfirms.remove(player.getUniqueId());
                player.sendMessage(ComponentColor.green("Cancelled pending action: %s.", Utils.formattedName(pend.command)));
                return true;
            }
        }

        if (pendingConfirms.containsKey(player.getUniqueId())) {
            player.sendMessage(
                ComponentColor.red("You have a pending action. Please use ")
                    .append(ComponentColor.darkRed("/customitem <confirm|cancel>"))
                    .append(ComponentColor.red(" to handle it."))
            );
            return true;
        }

        ItemStack hand = FLUtils.heldItem(player, EquipmentSlot.HAND);
        if (hand == null) hand = FLUtils.heldItem(player, EquipmentSlot.OFF_HAND);
        String key = args[1];
        Map<String, ItemStack> customItems = dh.getItems();

        switch (action) {
            case PUT -> {
                if (hand == null) {
                    player.sendMessage(ComponentColor.red("You must be holding an item in your hand."));
                    return true;
                }
                if (customItems.containsKey(key)) {
                    scheduleConfirm(player, action, key, hand);
                    player.sendMessage(
                        ComponentColor.red("The key '%s' already has a value. Run ", key)
                            .append(ComponentColor.darkRed("/customitem confirm"))
                            .append(ComponentColor.red(" to overwrite it, or "))
                            .append(ComponentColor.darkRed("/customitem cancel"))
                            .append(ComponentColor.red(" to cancel."))
                    );
                    return true;
                }
                handle(player, action, key, hand);
                return true;
            }
            case REMOVE -> {
                if (customItems.containsKey(key)) {
                    scheduleConfirm(player, action, key, null);
                    player.sendMessage(
                        ComponentColor.red("Are you sure you want to remove the key '%s'? Run ", key)
                            .append(ComponentColor.darkRed("/customitem confirm"))
                            .append(ComponentColor.red(" to confirm, or "))
                            .append(ComponentColor.darkRed("/customitem cancel"))
                            .append(ComponentColor.red(" to cancel."))
                    );
                    return true;
                }
                handle(player, action, key, null);
                return true;
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) {
        return switch (args.length) {
            case 1 -> Arrays.stream(Action.values()).map(Utils::formattedName).filter(s -> s.startsWith(args[0])).toList();
            case 2 -> switch (Utils.valueOfFormattedName(args[0], Action.class)) {
                case CANCEL, CONFIRM -> Collections.emptyList();
                default -> dh.getItems().keySet().stream().filter(s -> s.startsWith(args[1])).toList();
            };
            default -> Collections.emptyList();
        };
    }

    private void handle(Player player, Action action, String key, ItemStack item) {
        switch (action) {
            case PUT -> {
                dh.getItems().put(key, item.clone());
                player.sendMessage(ComponentColor.green("Put %s in key %s.", FLUtils.material(item), key));
            }
            case REMOVE -> {
                dh.getItems().remove(key);
                player.sendMessage(ComponentColor.green("Removed key %s", key));
            }
        }
    }

    private int requiredArgsLength(Action action) {
        return switch (action) {
            case CANCEL, CONFIRM, LIST, VP_REWARDS -> 1;
            default -> 2;
        };

    }

    private void scheduleConfirm(Player player, Action action, String key, ItemStack item) {
        int task = Bukkit.getScheduler().runTaskLater(
            FarLands.getInstance(),
            () -> pendingConfirms.remove(player.getUniqueId()),
            30 * 20 // 30 second timeout
        ).getTaskId();
        pendingConfirms.put(player.getUniqueId(), new PendingConfirm(task, action, key, item));
    }

    public enum Action {
        PUT, GET, REMOVE, CONFIRM, CANCEL, LIST, VP_REWARDS;
    }

    private record PendingConfirm(int taskId, Action command, String key, ItemStack item) {

    }

}
