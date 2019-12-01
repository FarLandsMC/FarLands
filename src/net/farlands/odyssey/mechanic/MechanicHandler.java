package net.farlands.odyssey.mechanic;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.mechanic.anticheat.AntiCheat;
import net.farlands.odyssey.mechanic.region.*;
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

        // Feature mechanics
        registerMechanic(new AFK());
        registerMechanic(new AntiCheat());
        registerMechanic(new Chat());
        registerMechanic(new CompassMechanic());
        registerMechanic(new GeneralMechanics());
        registerMechanic(new Restrictions());
        registerMechanic(new Spawn());
        registerMechanic(new Toggles());
        registerMechanic(new Voting());
        registerMechanic(new Items());

        Chat.log("Finished registering mechanics.");
    }

    private void registerMechanic(Mechanic mechanic) {
        mechanics.add(mechanic);
        Bukkit.getPluginManager().registerEvents(mechanic, FarLands.getInstance());
    }

    @SuppressWarnings("unchecked")
    public <T extends Mechanic> T getMechanic(Class<T> clazz) {
        return (T)mechanics.stream().filter(m -> m.getClass().equals(clazz)).findAny().orElse(null);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if(FarLands.class.equals(event.getPlugin().getClass()))
            mechanics.forEach(Mechanic::onStartup);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if(FarLands.class.equals(event.getPlugin().getClass()))
            mechanics.forEach(Mechanic::onShutdown);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        mechanics.forEach(mechanic -> mechanic.onPlayerJoin(event.getPlayer(),
                FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer()).secondsPlayed < 30));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        mechanics.forEach(mechanic -> mechanic.onPlayerQuit(event.getPlayer()));
    }
}
