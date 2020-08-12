package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class CommandKittyCannon extends PlayerCommand {
    public static final ItemStack CANNON;
    public static final Map<UUID, Ocelot> LIVE_ROUNDS;

    static {
        CANNON = new ItemStack(Material.DIAMOND_HORSE_ARMOR);
        ItemMeta meta = CANNON.getItemMeta();
        meta.setDisplayName(ChatColor.RESET.toString() + ChatColor.GOLD + "Kitty Cannon");
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        CANNON.setItemMeta(meta);
        LIVE_ROUNDS = new HashMap<>();
    }

    public CommandKittyCannon() {
        super(Rank.SPONSOR, Category.COSMETIC, "Launch a feline which explodes on impact.", "/kittycannon", "kittycannon");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Make sure the sender has exhausted the command cooldown
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        long timeRemaining = session.commandCooldownTimeRemaining(this);
        if (timeRemaining > 0) {
            sender.sendMessage(ChatColor.RED + "You can use this command again in " +
                    TimeInterval.formatTime(50L * timeRemaining, false));
            return true;
        }

        FLUtils.giveItem(sender, CANNON.clone(), true);
        session.setCommandCooldown(this, 10L * 60L * 20L);
        return true;
    }

    private static final double PI_ON_180 = Math.PI / 180;

    public static void fireCannon(Player player) {
        Location location = player.getLocation();
        double yaw = location.getYaw();
        double pitch = location.getPitch();
        double vx = -Math.sin(yaw * PI_ON_180) * Math.cos(pitch * PI_ON_180);
        double vy = -Math.sin(pitch * PI_ON_180);
        double vz = Math.cos(yaw * PI_ON_180) * Math.cos(pitch * PI_ON_180);
        double mag = 1.0 + 0.75 * Math.random();
        Vector velocity = new Vector(vx, vy, vz);

        Snowball snowball = (Snowball) player.getWorld().spawnEntity(player.getLocation().clone().add(0, 1, 0).add(velocity), EntityType.SNOWBALL);
        Ocelot kitty = (Ocelot) player.getWorld().spawnEntity(snowball.getLocation(), EntityType.OCELOT);
        kitty.setSilent(true);
        kitty.setInvulnerable(true);
        kitty.setRotation((float) (360 * Math.random()), 0.0f);
        snowball.addPassenger(kitty);
        snowball.setShooter(kitty);
        snowball.setVelocity(velocity.multiply(mag));
        LIVE_ROUNDS.put(snowball.getUniqueId(), kitty);
        player.playSound(player.getLocation(), Sound.ENTITY_OCELOT_AMBIENT, 3.0f, 1.0f);
    }
}
