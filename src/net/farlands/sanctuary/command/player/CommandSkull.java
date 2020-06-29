package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.kicas.rp.RegionProtection;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.TimeInterval;
import net.farlands.sanctuary.util.FLUtils;

import net.minecraft.server.v1_16_R1.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class CommandSkull extends PlayerCommand {
    public CommandSkull() {
        super(Rank.SAGE, Category.COSMETIC, "Give yourself a player's head.", "/skull <name> [amount]", "skull");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        long cooldownTime = session.commandCooldownTimeRemaining(this);
        if (cooldownTime > 0L) {
            sendFormatted(sender, "&(red)You can use this command again in %0.",
                    TimeInterval.formatTime(cooldownTime * 50L, false));
            return true;
        }
        session.setCommandCooldown(this, 400L);

        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sendFormatted(sender, "&(red)Invalid amount.");
                return true;
            }

            if (amount < 1)
                amount = 1;
        }

        net.minecraft.server.v1_16_R1.ItemStack skull = CraftItemStack.asNMSCopy(new ItemStack(Material.PLAYER_HEAD,
                args.length > 1 ? Math.min(8, amount) : 1));
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("SkullOwner", args[0]);
        skull.setTag(nbt);
        FLUtils.giveItem(sender, CraftItemStack.asBukkitCopy(skull), true);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}