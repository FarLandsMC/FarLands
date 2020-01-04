package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagList;
import net.minecraft.server.v1_14_R1.NBTTagString;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandShovel extends PlayerCommand {
    private final ItemStack shovel;

    public CommandShovel() {
        super(Rank.INITIATE, "Get a claim shovel.", "/shovel", "shovel");
        this.shovel = genShovel();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        long cooldownTime = session.commandCooldownTimeRemaining(this);
        if (cooldownTime > 0L) {
            sender.sendMessage(ChatColor.RED + "You can use this command again in " + TimeInterval.formatTime(cooldownTime * 50L, false) + ".");
            return true;
        }
        // Give the shovel and update the command cooldown
        session.setCommandCooldown(this, 10L * 60L * 20L);
        Utils.giveItem(sender, shovel.clone(), true);
        return true;
    }

    private static ItemStack genShovel() {
        net.minecraft.server.v1_14_R1.ItemStack shovel = CraftItemStack.asNMSCopy(new ItemStack(Material.GOLDEN_SHOVEL));
        NBTTagCompound nbt = new NBTTagCompound(), display = new NBTTagCompound();
        display.setString("Name", "{\"text\":\"" + ChatColor.RESET + ChatColor.AQUA + "Claim Shovel" + ChatColor.RESET + "\"}");
        NBTTagList lore = new NBTTagList();
        lore.add(new NBTTagString("Right-click to select the corners of your claim."));
        display.set("Lore", lore);
        nbt.set("display", display);
        shovel.setTag(nbt);
        return CraftItemStack.asBukkitCopy(shovel);
    }
}
