package net.farlands.sanctuary.mechanic.anticheat;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.index.qual.Positive;

import static org.bukkit.Material.*;

/**
 * Datastore for flight information
 */
public class FlightStore {

    private final Player  player;
    private final boolean sendAlerts;
    private       double  lastVy;
    private       int     strikes;
    private       boolean muted;

    private static final double JUMP_TOLERANCE           = (99.0 - FarLands.getFLConfig().getFDS()) * (2.0 / 990.0) + 0.25;
    private static final double VELOCITY_DELTA_TOLERANCE = (100 - FarLands.getFLConfig().getFDS()) / 400.0;
    private static final int    MAX_STRIKES              = 100 - FarLands.getFLConfig().getFDS();

    /**
     * Create flightstore for a player
     * @param player The player
     * @param sendAlerts If this player should generate alerts
     */
    public FlightStore(Player player, boolean sendAlerts) {
        this.player = player;
        this.sendAlerts = sendAlerts;
        this.lastVy = player.getVelocity().getY();
        this.strikes = 0;
        this.muted = false;
    }

    static {
        FarLands.getDebugger().post("flightDetect.velocityDeltaTolerance", VELOCITY_DELTA_TOLERANCE);
        FarLands.getDebugger().post("flightDetect.maxStrikes", MAX_STRIKES);
    }

    /**
     * Mute the flightstore for a certain amount of ticks
     */
    public void mute(@Positive long ticks) {
        boolean prev = this.muted;
        this.muted = true;
        Bukkit.getScheduler().runTaskLater(
            FarLands.getInstance(),
            () -> this.muted = prev,
            ticks
        );
    }

    /**
     * Update FlightStore
     */
    public void onUpdate() {
        if (isImmune()) {
            if (this.player.isOnGround()) {
                this.lastVy = Double.MAX_VALUE;
            }
            return;
        }

        double vy = this.player.getVelocity().getY();
        if (FLUtils.deltaEquals(this.lastVy, Double.MAX_VALUE, 10.0)) { // We're jumping
            PotionEffect jumpBoost = this.player.getPotionEffect(PotionEffectType.JUMP);
            // Calculates what the player's jump velocity should be, with some buffer to prevent false alarms (the +0.025)
            double vyMax = 0.41999998688697815 + 0.1 * (jumpBoost == null ? 0 : jumpBoost.getAmplifier() + 1) + JUMP_TOLERANCE;
            if (vy > vyMax && !FLUtils.checkNearby(this.player.getLocation(), SLIME_BLOCK, BUBBLE_COLUMN)) {
                if (this.sendAlerts && !this.muted) {
                    AntiCheat.broadcast(this.player.getName(), "jumped too high.");
                }
                if (!this.muted) {
                    FarLands.getDebugger().echo("vy: %f\nvyMax: %f".formatted(vy, vyMax));
                }
            }
            this.lastVy = vy;
            return;
        }

        // Update the last velocity one tick
        this.lastVy = (this.lastVy - 0.08) * 0.98;

        // Find the difference between the closest point on the expected curve within the next two seconds
        // To the actual velocity this update
        double lastDelta = Math.abs(this.lastVy - vy);
        for (int i = 0; i < 40; ++i) {
            this.lastVy = (this.lastVy - 0.08) * 0.98;
            if (Math.abs(this.lastVy - vy) >= lastDelta) {
                break;
            }
            lastDelta = Math.abs(this.lastVy - vy);
        }

        // If the percent difference between the closest point on the expected velocity curve to the current velocity
        // Is > 5%, they're probably flying
        flightCheck(lastDelta, Math.abs(vy));
        this.lastVy = vy;
    }

    /**
     * Run a flight check on the player
     */
    private void flightCheck(double lastDelta, double vy) {
        double pdiff = lastDelta / Math.abs(vy);
        if (pdiff > VELOCITY_DELTA_TOLERANCE) {
            ++this.strikes;
            FLPlayerSession session = FarLands.getDataHandler().getSession(this.player);
            if (this.strikes > MAX_STRIKES && session.flyAlertCooldown.isComplete()) {
                if (this.sendAlerts && !this.muted) {
                    AntiCheat.broadcast(session.handle.username, "might be flying.");
                    session = FarLands.getDataHandler().getSession(session.player);
                }

                if (!this.muted) {
                    FarLands.getDebugger().echo("pdiff: " + pdiff + "\nloc: " + FLUtils.coords(this.player.getLocation()));
                }

                session.flyAlertCooldown.reset();
            }
        } else {
            this.strikes = 0;
        }
    }

    /**
     * Check if the player is immune to flight detection
     */
    private boolean isImmune() {
        boolean immune = this.player.isOnGround() ||
                         this.player.isSwimming() ||
                         this.player.isGliding() ||
                         this.player.getVehicle() != null ||
                         this.player.isRiptiding() ||
                         this.player.hasPotionEffect(PotionEffectType.LEVITATION) ||
                         this.player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
                         AIR != this.player.getWorld().getBlockAt(this.player.getLocation()).getType() ||
                         FLUtils.checkNearby(this.player.getLocation(),
                                             WATER, LAVA, LADDER, VINE, COBWEB, HONEY_BLOCK,
                                             WEEPING_VINES, WEEPING_VINES_PLANT, TWISTING_VINES, TWISTING_VINES_PLANT
                         );

        if (immune) {
            return true;
        }

        FLPlayerSession session = FarLands.getDataHandler().getSession(player);
        immune = !session.flightDetectorMute.isComplete() || session.flying;

        if (immune) {
            return true;
        }

        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(player.getLocation());
        return flags != null && flags.isAllowed(RegionFlag.FLIGHT);
    }
}
