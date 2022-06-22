package net.farlands.sanctuary.data;

import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public enum Worlds {
    OVERWORLD("world"),
    NETHER("world_nether"),
    END("world_the_end"),
    FARLANDS("farlands", wc -> wc.generateStructures(true).seed(0xc0ffee)), // "party" world, used for events
    POCKET("pocket", true, wc -> {}),
    ;

    private static final List<String> names = Arrays.stream(values()).map(Worlds::getName).toList();

    /**
     * @return The names of the worlds in this enum
     */
    public static List<String> getNames() {
        return names;
    }

    public static Worlds getByName(String name) {
        return Arrays.stream(values()).filter(n -> n.name.equals(name)).findAny().orElse(null);
    }

    public static Worlds getByWorld(World world) {
        String name = world.getName();
        return Arrays.stream(values()).filter(n -> n.name.equals(name)).findAny().orElse(null);
    }

    private final String                 name;
    private final Consumer<WorldCreator> worldCreatorConfigurator;
    public final boolean enabled;

    Worlds(String name) {
        this.name = name;
        this.enabled = true;
        this.worldCreatorConfigurator = null;
    }

    /**
     * @param name                     The name of the world
     * @param worldCreatorConfigurator consumer to customise the settings of the {@link WorldCreator}
     */
    Worlds(String name, Consumer<WorldCreator> worldCreatorConfigurator) {
        this.name = name;
        this.enabled = true;
        this.worldCreatorConfigurator = worldCreatorConfigurator;
    }

    /**
     * @param name                     The name of the world
     * @param worldCreatorConfigurator consumer to customise the settings of the {@link WorldCreator}
     */
    Worlds(String name, boolean enabled, Consumer<WorldCreator> worldCreatorConfigurator) {
        this.name = name;
        this.enabled = enabled;
        this.worldCreatorConfigurator = worldCreatorConfigurator;
    }

    /**
     * @return The name of the world
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return The world that this represents
     */
    public World getWorld() {
        return Bukkit.getWorld(this.name);
    }

    /**
     * @param world The world to compare against
     * @return If the world provided matches names with this
     */
    public boolean matches(World world) {
        if(world == null) return false;
        return world.getName().equals(this.name);
    }

    /**
     * Creates the world if enum is declared with a configurator
     *
     * @return The new world if created otherwise null
     */
    public World createWorld() {
        if (this.worldCreatorConfigurator != null && this.enabled) {
            WorldCreator worldCreator = new WorldCreator(this.name);
            this.worldCreatorConfigurator.accept(worldCreator);
            return worldCreator.createWorld();
        }
        return null;
    }

    @Override
    public String toString() {
        return FLUtils.capitalize(this.name());
    }
}
