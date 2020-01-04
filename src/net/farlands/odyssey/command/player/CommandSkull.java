package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.NBTTagCompound;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class CommandSkull extends PlayerCommand {
    public CommandSkull() {
        super(Rank.SAGE, "Give yourself a player's head.", "/skull <name> [amount]", "skull");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;

        long cooldownTime = FarLands.getDataHandler().getRADH().cooldownTimeRemaining("skullCooldown", sender.getUniqueId().toString());
        if(cooldownTime > 0L) {
            sender.sendMessage(ChatColor.RED + "You can use this command again in " + TimeInterval.formatTime(cooldownTime * 50L, false) + ".");
            return true;
        }
        FarLands.getDataHandler().getRADH().setCooldown(400L, "skullCooldown", sender.getUniqueId().toString());

        int amount = 1;
        if(args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid amount");
                return true;
            }

            if(amount < 1)
                amount = 1;
        }

        net.minecraft.server.v1_14_R1.ItemStack skull = CraftItemStack.asNMSCopy(new ItemStack(Material.PLAYER_HEAD,
                args.length > 1 ? Math.min(8, amount) : 1));
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("SkullOwner", args[0]);
        skull.setTag(nbt);
        Utils.giveItem(sender, CraftItemStack.asBukkitCopy(skull), true);
        return true;
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }
}
