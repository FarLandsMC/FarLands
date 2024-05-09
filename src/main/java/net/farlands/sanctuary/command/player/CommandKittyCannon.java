package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;

import net.kyori.adventure.text.format.Style;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class CommandKittyCannon extends PlayerCommand {
    public static final ItemStack CANNON;
    public static final Map<UUID, Cat> LIVE_ROUNDS;

    static {
        CANNON = new ItemStack(Material.DIAMOND_HORSE_ARMOR);
        ItemMeta meta = CANNON.getItemMeta();
        meta.displayName(ComponentColor.gold("Kitty Cannon").style(Style.empty()));
        meta.addEnchant(Enchantment.INFINITY, 1, true);
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
            return error(sender, "You can use this command again in {}", TimeInterval.formatTime(50L * timeRemaining, false));
        }

        FLUtils.giveItem(sender, CANNON.clone(), true);
        session.setCommandCooldown(this, 10L * 60L * 20L);
        return true;
    }

    public static void fireCannon(Player player) {
        Location location = player.getLocation();
        double yaw = location.getYaw();
        double pitch = location.getPitch();
        double vx = -Math.sin(yaw   * FLUtils.DEGREES_TO_RADIANS) * Math.cos(pitch * FLUtils.DEGREES_TO_RADIANS);
        double vy = -Math.sin(pitch * FLUtils.DEGREES_TO_RADIANS);
        double vz =  Math.cos(yaw   * FLUtils.DEGREES_TO_RADIANS) * Math.cos(pitch * FLUtils.DEGREES_TO_RADIANS);
        double mag = 1.0 + 0.75 * Math.random();
        Vector velocity = new Vector(vx, vy, vz);

        Snowball snowball = (Snowball) player.getWorld().spawnEntity(player.getLocation().clone().add(0, 1, 0).add(velocity), EntityType.SNOWBALL);
        Cat kitty = (Cat) player.getWorld().spawnEntity(snowball.getLocation(), EntityType.CAT);
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
