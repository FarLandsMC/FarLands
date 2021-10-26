package net.farlands.sanctuary.mechanic.region;

import com.kicas.rp.data.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public abstract class ElytraCourse<T extends ElytraCourse.PlayerData> extends GameBase<T> {
    protected final List<Ring> rings;

    public ElytraCourse(Region region) {
        super(region);
        this.rings = new ArrayList<>();
    }

    protected void onPlayerPassThroughFirstRing(Player player) { }

    protected void onPlayerPassThroughRing(Player player, Ring ring) { }

    protected void onPlayerPassThroughFinalRing(Player player) { }

    @Override
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        super.onPlayerMove(event);

        if (!gameRegion.contains(event.getTo()))
            return;

        PlayerData pd = getData(event.getPlayer());
        if (pd == null)
            return;

        Ring targetRing = rings.get(pd.nextRing);
        if (targetRing.containsStep(event.getFrom(), event.getTo())) {
            if (pd.nextRing == 0)
                onPlayerPassThroughFirstRing(event.getPlayer());
            else if (pd.nextRing == rings.size() - 1) {
                onPlayerPassThroughFinalRing(event.getPlayer());
                return;
            } else
                onPlayerPassThroughRing(event.getPlayer(), targetRing);

            ++ pd.nextRing;
        }
    }

    protected static class PlayerData {
        protected int nextRing;

        PlayerData() {
            this.nextRing = 0;
        }
    }

    protected static class Ring {
        final Vector center;
        final Vector axis;
        final double radiusSquared;
        final double depth;

        Ring(Vector center, Vector axis, double radius, double depth) {
            this.center = center;
            this.axis = axis.normalize();
            this.radiusSquared = radius * radius;
            this.depth = depth;
        }

        Ring() {
            this(new Vector(), new Vector(), 0, 0);
        }

        boolean containsStep(Location from, Location to) {
            Vector step = from.toVector();
            Vector displacementDirection = to.toVector().subtract(step);
            double displacement = displacementDirection.length();
            displacementDirection.normalize();

            while (displacement > 0) {
                if (contains(step))
                    return true;

                step.add(displacementDirection);
                -- displacement;
            }

            return false;
        }

        boolean contains(Vector location) {
            Vector locationToCenter = location.clone().subtract(center);

            double distanceToCentralPlane = Math.abs(locationToCenter.dot(axis));
            if (distanceToCentralPlane > depth / 2)
                return false;

            double distanceToMajorAxis = locationToCenter.getCrossProduct(axis).lengthSquared();
            return distanceToMajorAxis <= radiusSquared;
        }
    }
}
