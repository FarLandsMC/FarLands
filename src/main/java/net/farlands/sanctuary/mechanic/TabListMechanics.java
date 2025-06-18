package net.farlands.sanctuary.mechanic;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.common.collect.ImmutableMap;
import com.kicas.rp.util.Pair;
import com.kicas.rp.util.ReflectionHelper;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.DataHandler;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public class TabListMechanics extends Mechanic {

    @Override
    public void onStartup() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), TabListMechanics::update, 0, 20L);
    }

    public static Pair<Component, Component> getHeaderFooter(boolean showVanished) {

        Map<Worlds, Integer> playersInWorlds = new HashMap<>();
        Set<Worlds> vanished = new HashSet<>();
        int online = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            OfflineFLPlayer session = FarLands.getDataHandler().getOfflineFLPlayer(player);
            Worlds world = Worlds.getByWorld(player.getWorld());

            if (session.vanished && showVanished) vanished.add(world);

            if (!session.vanished) ++online;

            if (!session.vanished || showVanished) playersInWorlds.compute(world, (k, v) -> v == null ? 1 : v + 1);
        }

        List<Component> worlds = playersInWorlds
            .entrySet()
            .stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
            .map(e -> {
                Component component = ComponentColor.color(
                    FLUtils.WORLD_COLORS.get(e.getKey()),
                    "{}: {}",
                    e.getKey().toString(), e.getValue()
                );
                if (vanished.contains(e.getKey())) return component.append(ComponentColor.gray("*"));
                return component;
            })
            .toList();

        Component header = ComponentColor.gold("- {} Player{0::s} Online -", online);

        Component footer = Component.join(JoinConfiguration.separator(ComponentColor.gray(" | ")), worlds)
            .append(!showVanished || vanished.isEmpty() ? Component.empty() : ComponentColor.gray("\n*Vanished"));


        return new Pair<>(header, footer);
    }


    public static void update() {
        update(false);
    }

    private static long lastUpdate = 0;
    public static void update(boolean force) {

        DataHandler dh = FarLands.getDataHandler();

        Pair<Component, Component> staff = getHeaderFooter(true);
        Pair<Component, Component> player = getHeaderFooter(false);

        Map<World, Component> mobcaps = new HashMap<>();
        AtomicReference<Component> lag = new AtomicReference<>();

        boolean updateHeaderFooter = force || System.currentTimeMillis() - lastUpdate >= 1000;
        if (updateHeaderFooter) {
            lastUpdate = System.currentTimeMillis();
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            OfflineFLPlayer flp = dh.getOfflineFLPlayer(p);
            FLPlayerSession session = dh.getSessionMap().get(flp.uuid);

            var name = Component.text();
            if (session.afk) name.append(ComponentColor.gray("[AFK] "));
            name.append(flp.getFullDisplayName(false));
            if (flp.vanished) name.append(ComponentColor.gray("*"));
            p.playerListName(name.asComponent());

            if (!updateHeaderFooter) return;

            List<Component> lines = new ArrayList<>();

            if (flp.tabMenuConfig.worlds()) {
                lines.add(flp.rank.isStaff() ? staff.getSecond() : player.getSecond());
            }

            if (flp.tabMenuConfig.lag()) {
                if (lag.get() == null) {
                    double mspt = FLUtils.serverMspt();
                    TextColor color = FLUtils.heatmapColor(mspt, 50);

                    lag.set(
                        ComponentColor.gray(
                            "TPS: {} MSPT: {}",
                            ComponentColor.color(color, "{::%.1f}", Bukkit.getTPS()[0]),
                            ComponentColor.color(color, "{::%.1f}", mspt)
                        )
                    );
                }
                lines.add(lag.get());
            }

            if (flp.tabMenuConfig.mobcaps()) {
                lines.add(
                    mobcaps.computeIfAbsent(p.getWorld(), w -> {
                        List<Component> comps = new ArrayList<>();
                        SpawnCategory[] values = { // Ordered array that contains all types that are valid
                            SpawnCategory.MONSTER,
                            SpawnCategory.ANIMAL,
                            SpawnCategory.AMBIENT,
                            SpawnCategory.AXOLOTL,
                            SpawnCategory.WATER_UNDERGROUND_CREATURE,
                            SpawnCategory.WATER_ANIMAL,
                            SpawnCategory.WATER_AMBIENT,
                        };

                        for (SpawnCategory cat : values) {
                            comps.add(getMobCap(p.getWorld(), cat));
                        }
                        return ComponentUtils.join(comps, ",").color(GRAY);
                    }));
            }

            p.sendPlayerListHeaderAndFooter(flp.rank.isStaff() ? staff.getFirst() : player.getFirst(), ComponentUtils.join(lines, "\n"));
        });
    }

    private static final Map<SpawnCategory, TextColor> categoryColors = ImmutableMap.<SpawnCategory, TextColor>builder()
        .put(SpawnCategory.MONSTER,       DARK_RED)
        .put(SpawnCategory.ANIMAL,        GREEN)
        .put(SpawnCategory.AMBIENT,       DARK_GRAY)
        .put(SpawnCategory.WATER_ANIMAL,  DARK_BLUE)
        .put(SpawnCategory.WATER_AMBIENT, DARK_AQUA)
        .build();

    private static final Map<SpawnCategory, MobCategory> categoryConversions = ImmutableMap.<SpawnCategory, MobCategory>builder()
        .put(SpawnCategory.MONSTER,                    MobCategory.MONSTER)
        .put(SpawnCategory.ANIMAL,                     MobCategory.CREATURE)
        .put(SpawnCategory.AMBIENT,                    MobCategory.AMBIENT)
        .put(SpawnCategory.AXOLOTL,                    MobCategory.AXOLOTLS)
        .put(SpawnCategory.WATER_UNDERGROUND_CREATURE, MobCategory.UNDERGROUND_WATER_CREATURE)
        .put(SpawnCategory.WATER_ANIMAL,               MobCategory.WATER_CREATURE)
        .put(SpawnCategory.WATER_AMBIENT,              MobCategory.WATER_AMBIENT)
        .put(SpawnCategory.MISC,                       MobCategory.MISC)
        .build();

    private static Component getMobCap(World world, SpawnCategory cat) {
        Class<?> craftworldclass = FLUtils.getCraftBukkitClass("CraftWorld");
        var level = (ServerLevel) ReflectionHelper.invoke("getHandle", craftworldclass, world);
        var lim = world.getSpawnLimit(cat);
        var s = level.getChunkSource().getLastSpawnState();
        var c = s.getMobCategoryCounts().getOrDefault(categoryConversions.get(cat), -1);
        return ComponentUtils.format(
            "{}/{}",
            c <= 0 ? ComponentColor.gray("-") : Component.text(c, FLUtils.heatmapColor(c, lim)),
            Component.text(lim, categoryColors.getOrDefault(cat, WHITE))
        );
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> TabListMechanics.update(true), 1);
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        Iterator<Player> iter = event.iterator();
        while (iter.hasNext()) {
            Player p = iter.next();
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(p);
            if (flp.vanished) {
                iter.remove();
            }
        }
    }

}
