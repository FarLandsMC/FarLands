package net.farlands.sanctuary.mechanic;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatMechanic;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.mechanic.region.AutumnEvent;
import net.farlands.sanctuary.mechanic.region.Spawn;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all plugin mechanics.
 */
public class MechanicHandler implements Listener {

    private final List<Mechanic> mechanics;

    public MechanicHandler() {
        this.mechanics = new ArrayList<>();
    }

    public void registerMechanics() { // Called in FarLands#onEnable
        Bukkit.getPluginManager().registerEvents(this, FarLands.getInstance());

        // Handlers
        registerMechanic(FarLands.getCommandHandler());
        registerMechanic(FarLands.getDataHandler());
        registerMechanic(FarLands.getGuiHandler());

        if (AutumnEvent.isActive()) { // Initialise the autumn event if it's active
            registerMechanic(new AutumnEvent());
        }

        // Feature mechanics
        registerMechanic(new AFK());
        registerMechanic(new AntiCheat());
        registerMechanic(new CompassMechanic());
        registerMechanic(new GeneralMechanics());
        registerMechanic(new Restrictions());
        registerMechanic(new Spawn());
        registerMechanic(new Toggles());
        registerMechanic(new VanillaFixes());
        registerMechanic(new Voting());
        registerMechanic(new Items());
        registerMechanic(new RotatingMessages());
        registerMechanic(new ChatMechanic());
        registerMechanic(new PocketMechanic());

        Logging.log("Finished registering mechanics.");
    }

    private void registerMechanic(Mechanic mechanic) {
        this.mechanics.add(mechanic);
        Bukkit.getPluginManager().registerEvents(mechanic, FarLands.getInstance());
    }

    /**
     * Get a mechanic by its class
     */
    @SuppressWarnings("unchecked")
    public <T extends Mechanic> T getMechanic(Class<T> clazz) {
        return (T) this.mechanics.stream().filter(m -> m.getClass().equals(clazz)).findAny().orElse(null);
    }

    /**
     * Calls the {@link Mechanic#onStartup} method for all register mechanics
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (FarLands.class.equals(event.getPlugin().getClass())) {
            this.mechanics.forEach(Mechanic::onStartup);
        }
    }

    /**
     * Calls the {@link Mechanic#onShutdown} method for all register mechanics
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (FarLands.class.equals(event.getPlugin().getClass())) {
            this.mechanics.forEach(Mechanic::onShutdown);
        }
    }

    /**
     * Calls the {@link Mechanic#onPlayerJoin} method for all register mechanics
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isNew = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer()).secondsPlayed < 10;
        this.mechanics.forEach(mechanic -> mechanic.onPlayerJoin(event.getPlayer(), isNew));
    }

    /**
     * Calls the {@link Mechanic#onPlayerQuit} method for all register mechanics
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.mechanics.forEach(mechanic -> mechanic.onPlayerQuit(event.getPlayer()));
    }
}
