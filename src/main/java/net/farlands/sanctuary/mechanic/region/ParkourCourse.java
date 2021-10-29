package net.farlands.sanctuary.mechanic.region;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;


import java.util.*;

/**
 * Create a parkour course.
 */
public class ParkourCourse extends Mechanic {
    private final String rewardSetName;
    private final List<Location> nodes;
    private final Map<UUID, Integer> nodeIndex;

    /**
     * Course constructor
     *
     * int[]... nodes   is to be passed in the order they are to be traversed
     *  Each node must contain {X, Y, Z} the block coordinates
     *  nodes[0]  must contain {X, Y, Z, pitch, yaw} additional rotation data
     *
     * @param world the world the course is in
     * @param nodes the coordinates of the nodes
     */
    public ParkourCourse(String rewardSetName, World world, int[]... nodes) {
        List<Location> nodesBuilder = new ArrayList<>();
        for (int[] arr : nodes)
            nodesBuilder.add(new Location(
                world,
                arr[0] + 0.5,
                arr[1] + 1.5,
                arr[2] + 0.5
            ));
        nodesBuilder.get(0).subtract(0, 1.5, 0);
        nodesBuilder.get(0).setPitch(nodes[0][3]);
        nodesBuilder.get(0).setYaw  (nodes[0][4]);

        this.rewardSetName = rewardSetName;
        this.nodes = nodesBuilder;
        this.nodeIndex = new HashMap<>();
    }

    public Location getSpawn() {
        return nodes.get(0).clone();
    }

    protected void onStart(Player player) {
        nodeIndex.put(player.getUniqueId(), 1);
        sendFormatted(player, "&(gold)Starting Parkour Course");
        player.playSound(
            player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1f, 1f
        );
    }
    protected void advanceNode(Player player) {
        int nextNode = nodeIndex.get(player.getUniqueId()) + 1;

        if (nextNode == nodes.size()) {
            onComplete(player);
            return;
        }

        nodeIndex.put(player.getUniqueId(), nextNode);
        player.playSound(
            player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1f, 1f
        );
    }
    protected void onFall(Player player) {
        nodeIndex.put(player.getUniqueId(), 0);
        sendFormatted(player, "&(red)Course Fail");
        player.playSound(
            player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 1f, 1f
        );
        FarLands.getScheduler().scheduleSyncDelayedTask(() -> {
            nodeIndex.remove(player.getUniqueId());
            FLUtils.tpPlayer(player, getSpawn());
        },20);
    }
    protected void onExit(Player player) {
        nodeIndex.remove(player.getUniqueId());
        sendFormatted(player, "&(gold)Leaving Parkour Course");
        player.playSound(
            player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1f, 1f
        );
    }
    protected void onComplete(Player player) {
        nodeIndex.remove(player.getUniqueId());
        sendFormatted(player, "&(gold)Parkour Course Complete!");
        player.playSound(
            player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f
        );
        FarLands.getDataHandler().getItemCollection(rewardSetName).onGameCompleted(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void update(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location toLocation = event.getTo();


        // not playing
        if (!nodeIndex.containsKey(player.getUniqueId())) {
            // at the beginning of the course
            if (isWithin(toLocation, getSpawn(), 3))
                onStart(player);
        }
        // playing
        else {
            int node = nodeIndex.get(player.getUniqueId());
            //  jumping                 fallen
            if (!player.isOnGround() || node <= 0)
                return;

            // outside of the course nodes
            if (!(isWithin(toLocation, nodes.get(node - 1), 4) ||
                  isWithin(toLocation, nodes.get(node),     4))) {
                if (node == 1)
                    onExit(player);
                else
                    onFall(player);
            }

            // reached the next node
            else if (isWithin(toLocation, nodes.get(node), node == nodes.size() - 1 ? 2 : 1.415f))
                advanceNode(player); // handles final node

            player.spawnParticle(Particle.REDSTONE, nodes.get(node), 1,
                    new Particle.DustOptions(Color.fromRGB(0x55FF55), 1));
        }
    }

    private static boolean isWithin(Location location, Location node, float range) {
        if (location == null || location.getWorld() == null ||
                node == null || node    .getWorld() == null ||
                !location.getWorld().getName().equals(node.getWorld().getName()))
            return false;
        return location.distance(node) <= range;
    }
}
