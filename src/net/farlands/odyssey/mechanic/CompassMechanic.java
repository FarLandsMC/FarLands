package net.farlands.odyssey.mechanic;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.PlayerDeath;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CompassMechanic extends Mechanic {

    private static final String[] deathStringIndex = {"", "second to ", "third to "};

    private Map<UUID, Integer> selectedCompass;

    public CompassMechanic() {
        this.selectedCompass = new HashMap<>();
    }

    private void pickCompass(Player player) {
        if (FarLands.getDataHandler().getDeaths(player.getUniqueId()).isEmpty()) {
            player.sendMessage(ChatColor.GOLD + "You haven't died yet!");
            return;
        }
        int death = (1 + selectedCompass.getOrDefault(player.getUniqueId(), 0)) % 3;
        selectedCompass.put(player.getUniqueId(), death);
        player.sendMessage(ChatColor.GRAY + "Compass pointed at your " + deathStringIndex[death] + "last death.");
        updateCompass(player, death);
    }

    private void updateCompass(Player player) {
        updateCompass(player, selectedCompass.getOrDefault(player.getUniqueId(), 0));
    }
    // From most recent death = 0..2
    private void updateCompass(Player player, int death) {
        List<PlayerDeath> deaths = FarLands.getDataHandler().getDeaths(player.getUniqueId());
        if (death >= deaths.size())
            return;
        Location location = deaths.get(deaths.size() - death - 1).getLocation();
        if (location != null)
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> player.setCompassTarget(location));
    }
    
    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.COMPASS)
            pickCompass(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Location location = event.getEntity().getLocation();
        event.getEntity().sendMessage(ChatColor.GRAY + "You died at " +
                location.getBlockX() + ", " +
                location.getBlockY() + ", " +
                location.getBlockZ());
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
