package net.farlands.sanctuary.mechanic;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.PlayerDeath;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles pointing a compass to a player's most recent death
 */
public class CompassMechanic extends Mechanic {

    private static final String[] deathStringIndex = { "", "second to ", "third to " };

    private final Map<UUID, Integer> selectedCompass;

    public CompassMechanic() {
        this.selectedCompass = new HashMap<>();
    }

    private void pickCompass(Player player) {
        if (FarLands.getDataHandler().getDeaths(player.getUniqueId()).isEmpty()) {
            player.sendMessage(ComponentColor.gold("You haven't died yet!"));
            return;
        }
        int death = (1 + this.selectedCompass.getOrDefault(player.getUniqueId(), 0)) % 3;
        this.selectedCompass.put(player.getUniqueId(), death);
        player.sendMessage(ComponentColor.gray("Compass pointed at your %s last death.", deathStringIndex[death]));
        updateCompass(player, death);
    }

    private void updateCompass(Player player) {
        updateCompass(player, this.selectedCompass.getOrDefault(player.getUniqueId(), 0));
    }

    /**
     * Update the compass to a different death
     *
     * @param player The player to update
     * @param death  The death
     */
    private void updateCompass(Player player, @Range(from = 0, to = 2) int death) {
        List<PlayerDeath> deaths = FarLands.getDataHandler().getDeaths(player.getUniqueId());
        if (death >= deaths.size()) {
            return;
        }
        Location location = deaths.get(deaths.size() - death - 1).location();
        if (location != null) {
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> player.setCompassTarget(location));
        }
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
            pickCompass(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getEntity().sendMessage(ComponentColor.gray("You died at %s", FLUtils.coords(event.getEntity().getLocation())));
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        updateCompass(player);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        updateCompass(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        updateCompass(event.getPlayer());
    }
}
