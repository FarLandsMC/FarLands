package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import net.farlands.odyssey.util.FLUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class CommandShovel extends PlayerCommand {
    private final ItemStack shovel;

    public CommandShovel() {
        super(Rank.INITIATE, "Get a claim shovel.", "/shovel", "shovel");
        this.shovel = genShovel();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Check cooldown
        long cooldownTime = session.commandCooldownTimeRemaining(this);
        if (cooldownTime > 0L) {
            TextUtils.sendFormatted(sender, "&(red)You can use this command again in %0",
                    TimeInterval.formatTime(cooldownTime * 50L, false));
            return true;
        }

        // Give the shovel and update the command cooldown
        FLUtils.giveItem(sender, shovel.clone(), true);
        session.setCommandCooldown(this, 10L * 60L * 20L);

        return true;
    }

    private static ItemStack genShovel() {
        ItemStack shovel = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Claim Shovel");
        meta.setLore(Collections.singletonList("Right-click to select the corners of your claim."));
        shovel.setItemMeta(meta);
        return shovel;
    }
}
