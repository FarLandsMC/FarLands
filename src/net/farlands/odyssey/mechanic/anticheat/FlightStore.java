package net.farlands.odyssey.mechanic.anticheat;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlightStore {
    private final Player player;
    private double lastVy;
    private int strikes;

    private static final double JUMP_TOLERANCE = (99.0 - FarLands.getFLConfig().getFDS()) * (2.0 / 990.0) + 0.025;
    private static final double VELOCITY_DELTA_TOLERANCE = (100 - FarLands.getFLConfig().getFDS()) / 400.0;
    private static final int MAX_STRIKES = 100 - FarLands.getFLConfig().getFDS();

    public FlightStore(Player player) {
        this.player = player;
        this.lastVy = player.getVelocity().getY();
        this.strikes = 0;
    }

    static {
        FarLands.getDebugger().post("flightDetect.velocityDeltaTolerance", VELOCITY_DELTA_TOLERANCE);
        FarLands.getDebugger().post("flightDetect.maxStrikes", MAX_STRIKES);
    }

    public void mute(long ticks) {
        FarLands.getDataHandler().getRADH().resetOrSetCooldown(ticks, "flightDetectMute", player.getUniqueId().toString(), null);
    }

    public void onUpdate() {
        if (isImmune(player)) {
            if (player.isOnGround())
                lastVy = Double.MAX_VALUE;
            return;
        }
        double vy = player.getVelocity().getY();
        if (Utils.deltaEquals(lastVy, Double.MAX_VALUE, 10.0)) { // We're jumping
            PotionEffect jumpBoost = player.getPotionEffect(PotionEffectType.JUMP);
            // Calculates what the player's jump velocity should be, with some buffer to prevent false alarms (the +0.025)
            double vyMax = 0.41999998688697815 + 0.1 * (jumpBoost == null ? 0 : jumpBoost.getAmplifier() + 1) + JUMP_TOLERANCE;
            if (vy > vyMax && !Utils.checkNearby(player.getLocation(), Material.SLIME_BLOCK, Material.BUBBLE_COLUMN)) {
                AntiCheat.broadcast(player.getName(), "jumped too high.");
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
        if(pdiff > VELOCITY_DELTA_TOLERANCE) {
            ++ strikes;
            if(strikes > MAX_STRIKES && FarLands.getDataHandler().getRADH().isCooldownComplete("flyAlert",
                    player.getUniqueId().toString())) {
                AntiCheat.broadcast(player.getName(), "might be flying.");
                FarLands.getDebugger().echo("pdiff", pdiff);
                FarLands.getDebugger().echo("loc", (int)player.getLocation().getX() + ", " + (int)player.getLocation().getY() +
                        ", " + (int)player.getLocation().getZ() + ", " + player.getLocation().getWorld().getName());
                FarLands.getDataHandler().getRADH().setCooldown(10L, "flyAlert", player.getUniqueId().toString());
            }
        } else
            strikes = 0;
    }

    private static boolean isImmune(Player player) {
        return player.isOnGround() || player.isSwimming() || player.isGliding() || player.getVehicle() != null || player.isRiptiding() ||
                player.hasPotionEffect(PotionEffectType.LEVITATION) || player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
                !Material.AIR.equals(player.getWorld().getBlockAt(player.getLocation()).getType()) ||
                Utils.checkNearby(player.getLocation(), Material.WATER, Material.LADDER, Material.VINE, Material.LAVA) ||
                !FarLands.getDataHandler().getRADH().isCooldownComplete("flightDetectMute", player.getUniqueId().toString());
    }
}
