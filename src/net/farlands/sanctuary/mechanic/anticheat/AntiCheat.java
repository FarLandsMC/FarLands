package net.farlands.sanctuary.mechanic.anticheat;

import com.kicas.rp.util.Pair;
import com.kicas.rp.util.TextUtils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.Logging;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AntiCheat extends Mechanic {
    private final Map<UUID, XRayStore> xray;
    private final Map<UUID, FlightStore> flight;

    public AntiCheat() {
        this.xray = new HashMap<>();
        this.flight = new HashMap<>();
    }

    public void muteFlightDetector(Player player, long ticks) {
        if(flight.containsKey(player.getUniqueId()))
            flight.get(player.getUniqueId()).mute(ticks);
    }

    public void put(Player player) {
        final boolean sendAlerts = !Rank.getRank(player).isStaff();
        xray.put(player.getUniqueId(), new XRayStore(player.getName(), sendAlerts));
        flight.put(player.getUniqueId(), new FlightStore(player, sendAlerts));
    }
    public void remove(Player player) {
        if (xray.containsKey(player.getUniqueId()))
            xray.get(player.getUniqueId()).printObtained();
        xray.remove(player.getUniqueId());
        flight.remove(player.getUniqueId());
    }

    public List<Pair<Detecting, Location>> getXRayNodes(UUID playerUUID) {
        if (xray.containsKey(playerUUID))
            return xray.get(playerUUID).getNodes();
        return null;
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        if (Rank.getRank(player).specialCompareTo(Rank.MEDIA) >= 0 && !FarLands.getDataHandler().getOfflineFLPlayer(player).debugging)
            return;
        put(player);
    }

    @Override
    public void onPlayerQuit(Player player) {
        remove(player);
    }

    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        if(event.getDamager().getType() == EntityType.ENDER_DRAGON && event.getEntityType() == EntityType.PLAYER)
            muteFlightDetector((Player)event.getEntity(), 10);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameMode.SURVIVAL.equals(event.getPlayer().getGameMode()) && xray.containsKey(event.getPlayer().getUniqueId()))
            xray.get(event.getPlayer().getUniqueId()).onBlockBreak(event);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if(!event.getChannel().equalsIgnoreCase("WDL|INIT"))
            return;
        broadcast(event.getPlayer().getName() + " was kicked for using World Downloader.", true);
        event.getPlayer().kickPlayer(ChatColor.RED + "Please disable World Downloader.");
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if(GameMode.SURVIVAL.equals(event.getPlayer().getGameMode()) && flight.containsKey(event.getPlayer().getUniqueId()))
            flight.get(event.getPlayer().getUniqueId()).onUpdate();
    }

    // Formats a message for Anti Cheat
    public static void broadcast(String message, boolean sendToAlerts) {
        Logging.broadcastStaff(ChatColor.RED + "[AC] " + message, null);
        if(sendToAlerts)
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.ALERTS, message);
    }

    public static void broadcast(String playerName, String message) {
        broadcast(playerName + " " + message, true);
        promptToSpec(playerName);
    }

    public static void promptToSpec(String playerName) {
        Logging.broadcastStaff(TextUtils.format(
                "&(aqua)$(hovercmd,/spec %0,{&(white)Teleport to %0 in spectator mode},Spectate [%0])",
                playerName
        ));
    }
}
