package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CommandShovel extends PlayerCommand {
    private final ItemStack shovel;

    public CommandShovel() {
        super(Rank.INITIATE, Category.UTILITY, "Get a claim shovel.", "/shovel", "shovel");
        this.shovel = genShovel();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Check cooldown
        long cooldownTime = session.commandCooldownTimeRemaining(this);
        if (cooldownTime > 0L) {
            error(sender, "You can use this command in {}.", TimeInterval.formatTime(cooldownTime * 50L, false));
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
        meta.displayName(ComponentColor.aqua("Claim Shovel").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            ComponentColor.gray("Right-Click to select the corners of your claim.").decoration(TextDecoration.ITALIC, false)
        ));
        shovel.setItemMeta(meta);
        return shovel;
    }
}
