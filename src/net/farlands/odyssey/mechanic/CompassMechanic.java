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

import java.util.List;

public class CompassMechanic extends Mechanic {
    
    private static void pickCompass(Player player) {
        if (FarLands.getDataHandler().getDeaths(player.getUniqueId()).isEmpty()) {
            player.sendMessage(ChatColor.GOLD + "You haven't died yet!");
            return;
        }
        int death = (1 + FarLands.getDataHandler().getRADH().retrieveInt("int", "compassDeathID")) % 3;
        player.sendMessage(ChatColor.GRAY + "Compass pointed at your "
                + (death > 0 ? (death == 1 ? "second" : "third") + " to " : "") + "last death.");
        updateCompass(player, death);
    }
    
    private static void updateCompass(Player player) {
        updateCompass(player, 0);
    }
    // From most recent death = 0..2
    private static void updateCompass(Player player, int death) {
        FarLands.getDataHandler().getRADH().store(death, "int", "compassDeathID");
        List<PlayerDeath> deaths = FarLands.getDataHandler().getDeaths(player.getUniqueId());
        if (death >= deaths.size()) { return; }
        Location l = deaths.get(deaths.size() - death - 1).getLocation();
        if (l != null)
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> player.setCompassTarget(l));
    }
    
    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.COMPASS) { pickCompass(event.getPlayer()); }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FarLands.getDataHandler().addDeath(event.getEntity());
        Location location = event.getEntity().getLocation();
        event.getEntity().sendMessage(ChatColor.GRAY + "You died at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
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
