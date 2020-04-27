package net.farlands.odyssey.mechanic.anticheat;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.util.FLUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlightStore {
    private final FLPlayerSession session;
    private final boolean sendAlerts;
    private double lastVy;
    private int strikes;

    private static final double JUMP_TOLERANCE = (99.0 - FarLands.getFLConfig().getFDS()) * (2.0 / 990.0) + 0.25;
    private static final double VELOCITY_DELTA_TOLERANCE = (100 - FarLands.getFLConfig().getFDS()) / 400.0;
    private static final int MAX_STRIKES = 100 - FarLands.getFLConfig().getFDS();

    public FlightStore(Player player, boolean sendAlerts) {
        this.session = FarLands.getDataHandler().getSession(player);
        this.sendAlerts = sendAlerts;
        this.lastVy = player.getVelocity().getY();
        this.strikes = 0;
    }

    static {
        FarLands.getDebugger().post("flightDetect.velocityDeltaTolerance", VELOCITY_DELTA_TOLERANCE);
        FarLands.getDebugger().post("flightDetect.maxStrikes", MAX_STRIKES);
    }

    public void mute(long ticks) {

    }

    public void onUpdate() {
        if (isImmune()) {
            if (session.player.isOnGround())
                lastVy = Double.MAX_VALUE;
            return;
        }
        double vy = session.player.getVelocity().getY();
        if (FLUtils.deltaEquals(lastVy, Double.MAX_VALUE, 10.0)) { // We're jumping
            PotionEffect jumpBoost = session.player.getPotionEffect(PotionEffectType.JUMP);
            // Calculates what the player's jump velocity should be, with some buffer to prevent false alarms (the +0.025)
            double vyMax = 0.41999998688697815 + 0.1 * (jumpBoost == null ? 0 : jumpBoost.getAmplifier() + 1) + JUMP_TOLERANCE;
            if (vy > vyMax && !FLUtils.checkNearby(session.player.getLocation(), Material.SLIME_BLOCK, Material.BUBBLE_COLUMN)) {
                if (sendAlerts)
                    AntiCheat.broadcast(session.player.getName(), "jumped too high.");
                FarLands.getDebugger().echo("vy", vy);
                FarLands.getDebugger().echo("vyMax", vyMax);
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
            if (strikes > MAX_STRIKES && session.flyAlertCooldown.isComplete()) {
                if (sendAlerts)
                    AntiCheat.broadcast(session.handle.username, "might be flying.");
                FarLands.getDebugger().echo("pdiff", pdiff);
                FarLands.getDebugger().echo("loc", (int) session.player.getLocation().getX() + ", " + (int) session.player.getLocation().getY() +
                        ", " + (int) session.player.getLocation().getZ() + ", " + session.player.getLocation().getWorld().getName());
                session.flyAlertCooldown.reset();
            }
        } else
            strikes = 0;
    }

    private boolean isImmune() {
        return  session.player.isOnGround() ||
                session.player.isSwimming() ||
                session.player.isGliding() ||
                session.player.getVehicle() != null ||
                session.player.isRiptiding() ||
                session.player.hasPotionEffect(PotionEffectType.LEVITATION) ||
                session.player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
                !Material.AIR.equals(session.player.getWorld().getBlockAt(session.player.getLocation()).getType()) ||
                FLUtils.checkNearby(session.player.getLocation(),
                        // TODO: add nether vines for 1.16 update
                        Material.WATER, Material.LAVA, Material.LADDER, Material.VINE, Material.COBWEB
                ) ||
                !session.flightDetectorMute.isComplete() ||
                session.flying;
    }
}
