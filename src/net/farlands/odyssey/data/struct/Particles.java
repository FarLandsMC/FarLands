package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.util.FLUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class Particles {
    private Particle type;
    private ParticleLocation location;

    public Particles(Particle type, ParticleLocation location) {
        this.type = type;
        this.location = location;
    }

    public Particle getType() {
        return type;
    }

    public ParticleLocation getLocation() {
        return location;
    }

    public void setTypeAndLocation(Particle type, ParticleLocation location) {
        this.type = type;
        this.location = location;
    }

    public void spawn(Player player) {
        double x = FLUtils.RNG.nextDouble() * 0.5 - 0.25, z = FLUtils.RNG.nextDouble() * 0.5 - 0.25;
        player.getWorld().spawnParticle(type, location.getLocation(player), 10, x, -1.0, z, 0.5);
    }

    public enum ParticleLocation {
        FEET(-0.25, false),
        TORSO(1.0, false),
        HEAD(0.5, true),
        ABOVE_HEAD(1.5, true);

        private final double yOffset;
        private final boolean fromEyeLocation;

        public static final ParticleLocation[] VALUES = values();

        ParticleLocation(double yOffset, boolean fromEyeLocation) {
            this.yOffset = yOffset;
            this.fromEyeLocation = fromEyeLocation;
        }

        public Location getLocation(Player player) {
            return (fromEyeLocation ? player.getEyeLocation() : player.getLocation()).add(0.0, yOffset, 0.0);
        }
    }
}
