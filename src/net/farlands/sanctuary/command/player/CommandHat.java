package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CommandHat extends PlayerCommand {
    public CommandHat() {
        super(Rank.DONOR, Category.COSMETIC, "Place the current item in your main hand on your head, or remove the current item.",
                "/hat|nohat", true, "hat", "nohat");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        PlayerInventory inv = sender.getInventory();
        if("hat".equals(args[0])) { // Swap hat with main hand
            ItemStack hand = nonNullStack(sender.getInventory().getItemInMainHand());
            inv.setItemInMainHand(nonNullStack(inv.getHelmet()));
            inv.setHelmet(hand);
        }else{ // Remove the hat
            inv.addItem(nonNullStack(inv.getHelmet()));
            inv.setHelmet(nonNullStack(null));
        }
        return true;
    }

    private static ItemStack nonNullStack(ItemStack stack) {
        return stack == null ? new ItemStack(Material.AIR, 1) : stack;
    }
}
