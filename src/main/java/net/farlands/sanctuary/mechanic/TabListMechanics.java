package net.farlands.sanctuary.mechanic;

import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.DataHandler;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class TabListMechanics extends Mechanic {

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

        List<TextComponent> worlds = playersInWorlds
            .entrySet()
            .stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
            .map(e -> {
                TextComponent component = Component.text(
                    FLUtils.capitalize(e.getKey().name()) + ": "
                    + e.getValue(), FLUtils.WORLD_COLORS.get(e.getKey())
                );
                if (vanished.contains(e.getKey())) return component.append(ComponentColor.gray("*"));
                return component;
            })
            .toList();

        Component header = ComponentColor.gold("- %s Player%s Online -", online, online == 1 ? "" : "s");

        Component footer = Component.join(JoinConfiguration.separator(ComponentColor.gray(" | ")), worlds)
            .append(!showVanished || vanished.isEmpty() ? Component.empty() : ComponentColor.gray("\n*Vanished"));


        return new Pair<>(header, footer);


    }

    public static void update() {
        DataHandler dh = FarLands.getDataHandler();

        Pair<Component, Component> staff = getHeaderFooter(true);
        Pair<Component, Component> player = getHeaderFooter(false);

        Bukkit.getOnlinePlayers().forEach(p -> {
            OfflineFLPlayer flp = dh.getOfflineFLPlayer(p);
            FLPlayerSession session = dh.getSessionMap().get(flp.uuid);

            List<ComponentLike> components = new ArrayList<>();

            components.add(flp.getFullDisplayName(false));

            if (session.afk) components.add(0, ComponentColor.gray("[AFK] "));
            if (flp.vanished) components.add(ComponentColor.gray("*"));

            p.playerListName(Component.join(JoinConfiguration.noSeparators(), components));

            if (flp.rank.isStaff()) {
                p.sendPlayerListHeaderAndFooter(staff.getFirst(), staff.getSecond());
            } else {
                p.sendPlayerListHeaderAndFooter(player.getFirst(), player.getSecond());
            }
        });
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), TabListMechanics::update, 1);
    }

}
