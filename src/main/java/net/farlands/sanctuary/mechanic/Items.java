package net.farlands.sanctuary.mechanic;

import com.comphenix.protocol.wrappers.WrappedBlockData;

import io.papermc.paper.persistence.PersistentDataContainerView;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandKittyCannon;
import net.farlands.sanctuary.data.pdc.JSONDataType;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.FireworkBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.Material.*;
import static org.bukkit.entity.EntityType.*;

/**
 * Handles events related to custom items
 */
public class Items extends Mechanic {

    private final Map<UUID, TNTArrow> tntArrows;

    private static final List<Material> UNBREAKABLE_BLOCKS = Arrays.asList(
            CHEST, ENDER_CHEST, BEDROCK, BARRIER, AIR
    );
    private static final List<EntityType> PASSIVES = Arrays.asList(
            PIG, COW, SHEEP, HORSE, LLAMA, OCELOT, SNOW_GOLEM, EntityType.CHICKEN, EntityType.RABBIT,
            SKELETON_HORSE, ZOMBIE_HORSE, MOOSHROOM
    );
    private static final List<EntityType> HOSTILES = Arrays.asList(
            ZOMBIE, BLAZE, SKELETON, CREEPER, SLIME, WITCH, VINDICATOR, SPIDER, CAVE_SPIDER, ENDERMAN, SILVERFISH,
            WITHER_SKELETON
    );

    public Items() {
        this.tntArrows = new HashMap<>();
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER) return; // If not shot by a player, return

        var pdc = event.getProjectile().getPersistentDataContainer();
        FarLands.getDebugger().echo("pdc: ", pdc);
        if (pdc != null && pdc.has(FLUtils.nsKey("tntArrow"))) {
            var tntArrow = pdc.get(FLUtils.nsKey("tntArrow"), new JSONDataType<>(TNTArrow.class));
            FarLands.getDebugger().echo("tnt arrow: ", tntArrow);
            event.setConsumeArrow(true);

            tntArrows.put(event.getProjectile().getUniqueId(), tntArrow);
        }
    }

    private static void entityExplosion(Location location, List<EntityType> selectionPool) {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < 15; ++i) {
            Entity entity = location.getWorld().spawnEntity(location, FLUtils.selectRandom(selectionPool));
            entity.setVelocity(new Vector(FLUtils.randomDouble(-1, 1), FLUtils.randomDouble(-1, 1), FLUtils.randomDouble(-1, 1)));
            entities.add(entity);
        }
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> entities.stream().filter(Entity::isValid).forEach(Entity::remove), 60 * 20L);

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (CommandKittyCannon.LIVE_ROUNDS.containsKey(event.getEntity().getUniqueId())) {
            CommandKittyCannon.LIVE_ROUNDS.get(event.getEntity().getUniqueId()).setHealth(0.0);
            Location loc = event.getEntity().getLocation();
            event.getEntity().getWorld().createExplosion(loc.clone(), 0.0f);
            event.getEntity().getWorld().spawnParticle(Particle.EXPLOSION, loc, 15, 4.0, 4.0, 4.0);
            event.getEntity().remove();
            Bukkit.getScheduler().runTaskLater(
                    FarLands.getInstance(),
                    () -> CommandKittyCannon.LIVE_ROUNDS.remove(event.getEntity().getUniqueId()),
                    20L
            );
            return;
        }

        // TODO: This doesn't work because the pdc of the arrow entity is not the same as the item.
        PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        if (pdc.has(FLUtils.nsKey("tntArrow"))) {
            TNTArrow data = pdc.get(FLUtils.nsKey("tntArrow"), new JSONDataType<>(TNTArrow.class));
            switch (data.type) {
                case 0:
                    fakeExplosion(event.getEntity().getLocation(), data.strength * 5.0, data.duration);
                    break;
                case 1:
                    FarLands.getDebugger().echo("normal arrow");
                    event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), data.strength * 4.0F, true);
                    break;
                case 2: {
                    Location loc = event.getEntity().getLocation();
                    FireworkBuilder.randomFirework(1, 1, 1).spawnEntity(loc);
                    double dx, dy, dz;
                    for (int i = 0; i < 2; ++i) {
                        double theta = FLUtils.RNG.nextDouble() * 2 * Math.PI;
                        dx = FLUtils.randomDouble(1.5, 2.5) * Math.cos(theta);
                        dy = FLUtils.randomDouble(2, 3) * Math.sin(FLUtils.RNG.nextDouble() * 0.5 * Math.PI);
                        dz = FLUtils.randomDouble(1.5, 2.5) * Math.sin(theta);
                        FireworkBuilder.randomFirework(1, 1, 1).spawnEntity(loc.clone().add(dx, dy, dz));
                    }
                    break;
                }
                case 3: {
                    entityExplosion(event.getEntity().getLocation().add(0, 1, 0), PASSIVES);
                    break;
                }
                case 4: {
                    entityExplosion(event.getEntity().getLocation().add(0, 1, 0), HOSTILES);
                    break;
                }
                case 5: {
                    Location loc = event.getEntity().getLocation().add(0, 3, 0);
                    double speed = Math.min(2, Math.max(0.3, data.strength * 0.25));
                    for (int i = 0; i < data.strength * 5; ++i) {
                        TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(loc, EntityType.TNT);
                        tnt.setFuseTicks(100); // 5 seconds
                        tnt.setVelocity(new Vector(speed * FLUtils.randomDouble(-1, 1), speed * FLUtils.randomDouble(-1, 1),
                                speed * FLUtils.randomDouble(-1, 1)));
                    }
                    break;
                }
                case 6: {
                    final Location loc = event.getEntity().getLocation().clone();
                    loc.setY(loc.getWorld().getMaxHeight() * 0.75);
                    final double maxRadius = Math.min(15, data.strength * 3);
                    final int taskId = FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
                        double theta = FLUtils.randomDouble(0, 2 * Math.PI), radius = FLUtils.randomDouble(0, maxRadius);
                        TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(loc.clone().add(radius * (2 * Math.cos(theta) - 1), 0,
                                radius * (2 * Math.sin(theta) - 1)), EntityType.TNT);
                        tnt.setFuseTicks(200); // 10 seconds
                    }, 0, 2);
                    FarLands.getScheduler().scheduleSyncDelayedTask(() -> FarLands.getScheduler().cancelTask(taskId), (long) (data.strength * 81));
                    break;
                }
            }
            event.getEntity().remove();
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> tntArrows.remove(event.getEntity().getUniqueId()), 1L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        if (
                (tntArrows.containsKey(event.getDamager().getUniqueId()) && tntArrows.get(event.getDamager().getUniqueId()).type == 2) ||
                CommandKittyCannon.LIVE_ROUNDS.containsKey(event.getDamager().getUniqueId())
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Cat &&
                CommandKittyCannon.LIVE_ROUNDS.containsValue((Cat)event.getEntity())) // says cast is redundant - warns without it
            event.getDrops().clear();
        // else
        //     it's a pretty good string farm
    }

    private void fakeExplosion(Location location, double radius, int duration) {
        List<Player> players = location.getWorld().getEntities().stream().filter(ent -> EntityType.PLAYER == ent.getType() &&
                ent.getLocation().distanceSquared(location) < 75 * 75).map(ent -> (Player) ent).collect(Collectors.toList());
        if (radius <= 10.0) {
            Map<Block, WrappedBlockData> exploded = getExplodedBlocks(location, radius);
            players.forEach(player -> {
                FLUtils.changeBlocks(player, exploded);
                player.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 5.0F, 1.0F);
            });
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                Map<Block, WrappedBlockData> reset = new HashMap<>();
                exploded.keySet().forEach(block -> reset.put(block, WrappedBlockData.createData(block.getBlockData())));
                players.forEach(player -> FLUtils.changeBlocks(player, reset));
            }, duration * 20L);
        } else {
            (new Thread(() -> {
                Map<Block, WrappedBlockData> exploded = getExplodedBlocks(location, radius);
                players.forEach(player -> {
                    FLUtils.changeBlocksAsync(player, exploded);
                    player.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 5.0F, 1.0F);
                });
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    Map<Block, WrappedBlockData> reset = new HashMap<>();
                    exploded.keySet().forEach(block -> reset.put(block, WrappedBlockData.createData(block.getBlockData())));
                    players.forEach(player -> FLUtils.changeBlocksAsync(player, reset));
                }, duration * 20L);
            })).start();
        }
    }

    private Map<Block, WrappedBlockData> getExplodedBlocks(Location location, double radius) {
        final double r2 = radius * radius;
        Map<Block, WrappedBlockData> exploded = new HashMap<>();
        for (double y = -radius; y < radius; ++y) {
            for (double x = -radius; x < radius; ++x) {
                for (double z = -radius; z < radius; ++z) {
                    Block current = location.clone().add(x, y, z).getBlock();
                    if (!UNBREAKABLE_BLOCKS.contains(current.getType()) && current.getLocation().distanceSquared(location) <= r2) {
                        Location loc = current.getLocation().clone().subtract(0, 1, 0);
                        exploded.put(current, WrappedBlockData.createData((loc.distanceSquared(location) > r2 ||
                                UNBREAKABLE_BLOCKS.contains(loc.getBlock().getType())) && loc.getBlock().getType().isSolid() &&
                                FLUtils.randomChance(0.2) ? Material.FIRE : Material.AIR));
                    }
                }
            }
        }
        return exploded;
    }

    public static final class TNTArrow {
        public float strength;
        public int duration;
        public int type;
    }
}
