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

import static org.bukkit.Material.*;

/**
 * Checks for players fly hacking.
 */
public class FlightStore {
    private final Player player;
    private final boolean sendAlerts;
    private double lastVy;
    private int strikes;
    private boolean muted;

    private static final double JUMP_TOLERANCE = (99.0 - FarLands.getFLConfig().getFDS()) * (2.0 / 990.0) + 0.25;
    private static final double VELOCITY_DELTA_TOLERANCE = (100 - FarLands.getFLConfig().getFDS()) / 400.0;
    private static final int MAX_STRIKES = 100 - FarLands.getFLConfig().getFDS();

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

    public void mute(long ticks) {
        boolean prev = this.muted;
        this.muted = true;
        Bukkit.getScheduler().runTaskLater(
            FarLands.getInstance(),
            () -> this.muted = prev,
            ticks
        );
    }

    public void onUpdate() {
        if (isImmune()) {
            if (player.isOnGround())
                lastVy = Double.MAX_VALUE;
            return;
        }

        double vy = player.getVelocity().getY();
        if (FLUtils.deltaEquals(lastVy, Double.MAX_VALUE, 10.0)) { // We're jumping
            PotionEffect jumpBoost = player.getPotionEffect(PotionEffectType.JUMP);
            // Calculates what the player's jump velocity should be, with some buffer to prevent false alarms (the +0.025)
            double vyMax = 0.41999998688697815 + 0.1 * (jumpBoost == null ? 0 : jumpBoost.getAmplifier() + 1) + JUMP_TOLERANCE;
            if (vy > vyMax && !FLUtils.checkNearby(player.getLocation(), SLIME_BLOCK, BUBBLE_COLUMN)) {
                if (sendAlerts && !muted)
                    AntiCheat.broadcast(player.getName(), "jumped too high.");
                if(!muted)
                FarLands.getDebugger().echo("vy: %f\nvyMax: %f".formatted(vy, vyMax));
            }
            lastVy = vy;
            return;
        }

        // Update the last velocity one tick
        lastVy = (lastVy - 0.08) * 0.98;

        // Find the difference between the closest point on the expected curve within the next two seconds
        // To the actual velocity this update
        double lastDelta = Math.abs(lastVy - vy);
        for (int i = 0; i < 40; ++i) {
            lastVy = (lastVy - 0.08) * 0.98;
            if (Math.abs(lastVy - vy) >= lastDelta)
                break;
            lastDelta = Math.abs(lastVy - vy);
        }

        // If the percent difference between the closest point on the expected velocity curve to the current velocity
        // Is > 5%, they're probably flying
        flightCheck(lastDelta, Math.abs(vy));
        lastVy = vy;
    }

    private void flightCheck(double lastDelta, double vy) {
        double pdiff = lastDelta / Math.abs(vy);
        if (pdiff > VELOCITY_DELTA_TOLERANCE) {
            ++strikes;
            FLPlayerSession session = FarLands.getDataHandler().getSession(player);
            if (strikes > MAX_STRIKES && session.flyAlertCooldown.isComplete()) {
                if (sendAlerts && !muted) {
                    AntiCheat.broadcast(session.handle.username, "might be flying.");
                    session = FarLands.getDataHandler().getSession(session.player);
                }

                if (!muted) {
                    FarLands.getDebugger().echo("pdiff: " + pdiff + "\nloc: " + FLUtils.coords(player.getLocation()));
                }

                session.flyAlertCooldown.reset();
            }
        } else
            strikes = 0;
    }

    private boolean isImmune() {
        boolean immune = player.isOnGround() ||
                player.isSwimming() ||
                player.isGliding() ||
                player.getVehicle() != null ||
                player.isRiptiding() ||
                player.hasPotionEffect(PotionEffectType.LEVITATION) ||
                player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
                AIR != player.getWorld().getBlockAt(player.getLocation()).getType() ||
                FLUtils.checkNearby(player.getLocation(),
                        WATER, LAVA, LADDER, VINE, COBWEB, HONEY_BLOCK,
                        WEEPING_VINES, WEEPING_VINES_PLANT, TWISTING_VINES, TWISTING_VINES_PLANT
                );

        if (immune)
            return true;

        FLPlayerSession session = FarLands.getDataHandler().getSession(player);
        immune = !session.flightDetectorMute.isComplete() || session.flying;

        if (immune)
            return true;

        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(player.getLocation());
        return flags != null && flags.isAllowed(RegionFlag.FLIGHT);
    }
}
