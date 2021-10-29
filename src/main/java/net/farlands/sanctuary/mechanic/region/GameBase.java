package net.farlands.sanctuary.mechanic.region;

import com.kicas.rp.data.Region;
import net.farlands.sanctuary.mechanic.Mechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for plugin games.
 * @param <T> the game
 */
public abstract class GameBase<T> extends Mechanic {
    protected final Map<Player, T> playerData;
    protected final Region gameRegion;

    protected GameBase(Region gameRegion) {
        this.playerData = new HashMap<>();
        this.gameRegion = gameRegion;
    }

    protected void onPlayerExitGame(Player player) { }

    protected void onPlayerEnterGameRegion(Player player) { }

    protected void onPlayerExitGameRegion(Player player) { }

    protected void addPlayer(Player player, T data) {
        playerData.put(player, data);
    }

    protected boolean isPlaying(Player player) {
        return playerData.containsKey(player);
    }

    protected T getData(Player player) {
        return playerData.get(player);
    }

    protected void removePlayer(Player player) {
        playerData.remove(player);
    }

    @Override
    public void onPlayerQuit(Player player) {
        onPlayerExitGame(player);
        playerData.remove(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameRegion.contains(event.getFrom()) && !gameRegion.contains(event.getTo()))
            onPlayerExitGameRegion(event.getPlayer());
        else if (!gameRegion.contains(event.getFrom()) && gameRegion.contains(event.getTo()))
            onPlayerEnterGameRegion(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        onPlayerMove(event);
    }
}
