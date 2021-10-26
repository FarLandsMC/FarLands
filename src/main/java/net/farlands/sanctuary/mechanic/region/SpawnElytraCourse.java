package net.farlands.sanctuary.mechanic.region;

import com.kicas.rp.data.Region;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.struct.ItemCollection;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.FireworkBuilder;
import net.farlands.sanctuary.util.FireworkExplosionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

public class SpawnElytraCourse extends ElytraCourse<SpawnElytraCourse.PlayerData> {
    // TODO: Get start location
    private static final Location START = new Location(Bukkit.getWorld("world"), 0, 0, 0);
    private final ItemCollection rewards;

    public SpawnElytraCourse() {
        // TODO: define elytra course region
        super(new Region(new Location(Bukkit.getWorld("world"), 0, 0, 0), new Location(Bukkit.getWorld("world"), 0, 0, 0)));
        this.rings.addAll(RINGS);
        this.rewards = FarLands.getDataHandler().getItemCollection("spawnElytraCourse");
    }

    @Override
    protected void onPlayerEnterGameRegion(Player player) {
        addPlayer(player, new PlayerData());
        player.sendMessage(ChatColor.GOLD + "Welcome to the spawn elytra course! You have five seconds to fly from " +
                "each ring to the next. Make it to the end to receive a prize!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
    }

    @Override
    protected void onPlayerExitGameRegion(Player player) {
        removePlayer(player);
        player.sendMessage(ChatColor.GOLD + "Exiting the elytra course.");
    }

    @Override
    protected void onPlayerPassThroughRing(Player player, Ring ring) {
        PlayerData pd = getData(player);
        pd.timer.reset(() -> SpawnElytraCourse.this.failCourse(player));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
    }

    @Override
    protected void onPlayerPassThroughFinalRing(Player player) {
        PlayerData pd = getData(player);
        pd.reset();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);

        rewards.onGameCompleted(player);

        // Spawn fireworks
        Vector center = rings.get(rings.size() - 1).center;
        for (int i = 0; i < 15; ++i) {
            double theta = FLUtils.RNG.nextDouble() * 2 * Math.PI,
                    omega = FLUtils.RNG.nextDouble() * 2 * Math.PI;
            double dx = Math.cos(theta) * FLUtils.randomDouble(0, 12),
                    dy = Math.sin(theta) * FLUtils.randomDouble(0, 12),
                    dz = Math.cos(omega) * FLUtils.randomDouble(0, 12);
            // Some shade of blue
            int[] rgb = new int[] {FLUtils.randomInt(0, 48), FLUtils.randomInt(0, 175), FLUtils.randomInt(128, 256)};
            (new FireworkBuilder(1, 1))
                    .addExplosion(FireworkExplosionType.randomType(FLUtils.RNG), rgb)
                    .setFadeColors(rgb)
                    .spawnEntity(new Location(Bukkit.getWorld("world"), center.getX(), center.getY(), center.getZ())
                            .clone().add(dx, dy, dz));
        }
    }

    private void failCourse(Player player) {
        PlayerData pd = getData(player);
        if (pd != null) {
            player.sendMessage(ChatColor.RED + "You did not complete the elytra course! Resetting...");
            player.setGliding(false);
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 5.0F, 1.0F);
            pd.reset();
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                player.setGliding(false);
                FLUtils.tpPlayer(player, START);
            }, 20);
        }
    }

    protected class PlayerData extends ElytraCourse.PlayerData {
        Cooldown timer;

        PlayerData() {
            super();
            // 5 seconds to get to the next loop
            this.timer = new Cooldown(5 * 20);
        }

        void reset() {
            timer.cancel();
            nextRing = 0;
        }
    }

    private static final List<Ring> RINGS = Arrays.asList(
            // TODO: generate rings
    );
}
