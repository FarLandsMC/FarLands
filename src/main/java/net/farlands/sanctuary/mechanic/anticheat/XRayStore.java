package net.farlands.sanctuary.mechanic.anticheat;

import com.kicas.rp.util.Materials;
import com.kicas.rp.util.Pair;

import net.farlands.sanctuary.FarLands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;

import static java.lang.Math.abs;

import java.util.*;

/**
 * Handles x-ray prevention and detection.
 */
public class XRayStore {

    private static final List<BlockFace> SIDES = Arrays.asList(
        BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP
    );
    public static final List<Material> ORES; // Used in /mined

    static {
        ORES = Materials.materialsEndingWith("_ORE");
        ORES.add(Material.ANCIENT_DEBRIS);
        //ORES.add(Material.AMETHYST_BLOCK);
        //ORES.add(Material.BUDDING_AMETHYST);
    }


    private final String playerName;
    private final boolean sendAlerts;
    private int strikes;
    private Location last;
    private int mined;
    private final HashMap<Material, Integer> obtained;
    private final List<Pair<Detecting, Location>> nodes;

    public XRayStore(String playerName, boolean sendAlerts) {
        this.playerName = playerName;
        this.sendAlerts = sendAlerts;
        this.strikes    = 0;
        this.last       = null;
        this.mined      = 0;
        this.obtained   = new HashMap<>();
        this.nodes      = new ArrayList<>();
    }

    public void onBlockBreak(BlockBreakEvent event) {
        Block minedBlock = event.getBlock();

        // log all mined ores to flush on logout
        if (ORES.contains(minedBlock.getType()))
            obtained.put(minedBlock.getType(), obtained.getOrDefault(minedBlock.getType(), 0) + 1);

        Block exposedBlock;
        // Are we exposing an ore that we want to track
        for (Detecting loopDetecting : Detecting.values()) {
            for (BlockFace blockFace : SIDES) {
                exposedBlock = minedBlock.getRelative(blockFace);
                if (loopDetecting.isValid(exposedBlock)) {
                    onOreExposed(event, loopDetecting, exposedBlock);
                    return;
                }
            }
        }

        // Are we mining an ore that we want to track
        for (Detecting loopDetecting : Detecting.values()) {
            if (loopDetecting.isValid(minedBlock)) {
                onOreMined(loopDetecting, minedBlock);
                return;
            }
        }

        // If there are no ores exposed
        ++mined;
    }

    public void printObtained() {
        if (obtained.isEmpty())
            return;

        StringBuilder debugMessage = new StringBuilder();
        debugMessage.append(playerName).append(" has broken the following blocks this session:\n");

        for (Map.Entry<Material, Integer> entry : obtained.entrySet())
            debugMessage.append(" ").append(entry.getKey().name().toLowerCase()).append(" * ").append(entry.getValue()).append("\n");

        FarLands.getDebugger().echo(debugMessage.toString().trim());
    }

    public List<Pair<Detecting, Location>> getNodes() {
        return nodes;
    }

    private boolean checkFirst(Block block, Detecting detecting, StringBuilder debugMessage, StringBuilder alertMessage) {
        if (last != null)
            return false;

        last = block.getLocation();
        debugMessage.append(playerName).append(" : ")
                    .append(detecting.name().toLowerCase()).append(" : ")
                    .append(last.getBlockX()).append(" ")
                    .append(last.getBlockY()).append(" ")
                    .append(last.getBlockZ()).append(" @ First Node");
        alertMessage.append(playerName).append(" has found a vein of ")
                    .append(detecting.color).append(detecting.name().toLowerCase())
                    .append(ChatColor.RED).append(" @ ")
                    .append(last.getBlockX()).append(" ")
                    .append(last.getBlockY()).append(" ")
                    .append(last.getBlockZ());
        nodes.add(0, new Pair<>(detecting, block.getLocation()));
        return true;
    }

    private void finish(Block minedBlock, Block block, Detecting detecting) {
        final int dx = block.getLocation().getBlockX() - last.getBlockX(),
                  dy = block.getLocation().getBlockY() - last.getBlockY(),
                  dz = block.getLocation().getBlockZ() - last.getBlockZ();
        // ignore blocks that are mined nearby as they are probably from the same vein
        if (abs(dy) <= 2 && abs(dx) <= 2 && abs(dz) <= 2)
            return;

        nodes.add(0, new Pair<>(detecting, block.getLocation()));
        final String oreType = detecting.color + detecting.toString();
        StringBuilder debugMessage = new StringBuilder(),
                alertMessage = new StringBuilder();

        last = block.getLocation();
        debugMessage.append(playerName).append(" : ")
                    .append(detecting.name().toLowerCase()).append(" : ")
                    .append(last.getBlockX()).append(" ")
                    .append(last.getBlockY()).append(" ")
                    .append(last.getBlockZ()).append(" @ ")
                    .append(dx).append(" ")
                    .append(dy).append(" ")
                    .append(dz);
        alertMessage.append(playerName).append(" has found a vein of ")
                    .append(oreType).append(ChatColor.RED).append(" @ ")
                    .append(last.getBlockX()).append(" ")
                    .append(last.getBlockY()).append(" ")
                    .append(last.getBlockZ());

        // Minimal path is |dy| + 2(|dx|+|dz|) // * 2.1 ( 4 + rather than 2 to account for an imperfect path
        // We also ignore anything below half the minimal path to account for caving
        int a = abs(dx) + abs(dy) + abs(dz),
            b = abs(dy) + (int)Math.ceil(2.1 * (4 + abs(dx) + abs(dz)));
        debugMessage.append("\nmined : ").append(a).append(" ").append(mined).append(" ").append(b);

        if ((abs(dy) <= 3 && (abs(dx) <= 2 || abs(dz) <= 2)) || (a >= mined || mined >= b)) {
            FarLands.getDebugger().echo(debugMessage.toString());
            if (sendAlerts)
                AntiCheat.broadcast(alertMessage.toString(), false);
        } else {
            alertMessage.setLength(0);
            if (ORES.contains(minedBlock.getType())) {
                debugMessage.append("\nStrikes : ").append(++strikes);
                alertMessage.append(playerName).append(" might be using X-Ray");
            } else {
                debugMessage.append("\nHidden Node");
                alertMessage.append(playerName).append(" has found a hidden ").append(oreType);
            }
            FarLands.getDebugger().echo(debugMessage.toString());
            if (sendAlerts) {
                AntiCheat.sendDiscordAlert(alertMessage.toString());
                AntiCheat.broadcast(alertMessage.append(ChatColor.RED).append(" @ ")
                        .append(last.getBlockX()).append(" ")
                        .append(last.getBlockY()).append(" ")
                        .append(last.getBlockZ()).toString(), false);
                AntiCheat.promptToSpec(playerName);
            }
        }

        mined = 0;
    }

    private void onOreExposed(BlockBreakEvent event, Detecting detecting, Block exposedBlock) {
        StringBuilder debugMessage = new StringBuilder(),
                      alertMessage = new StringBuilder();

        if (checkFirst(exposedBlock, detecting, debugMessage, alertMessage)) {
            final boolean hidden = !ORES.contains(event.getBlock().getType());
            if (hidden) {
                debugMessage.append("\nHidden Node");
                alertMessage.setLength(0);
                alertMessage.append(playerName).append(" has found a hidden ")
                        .append(detecting.color).append(detecting.toString());
            }

            FarLands.getDebugger().echo(debugMessage.toString());
            if (sendAlerts) {
                // TODO: re-enable after persistence update
                // FarLands.getDiscordHandler().sendMessage("alerts", alertMessage.toString());
                AntiCheat.broadcast(alertMessage.append(ChatColor.RED).append(" @ ")
                        .append(last.getBlockX()).append(" ")
                        .append(last.getBlockY()).append(" ")
                        .append(last.getBlockZ()).toString(), false);
                if (hidden)
                    AntiCheat.promptToSpec(playerName);
            }

            mined = 0;
            return;
        }

        finish(event.getBlock(), exposedBlock, detecting);
    }

    private void onOreMined(Detecting detecting, Block minedBlock) {
        StringBuilder debugMessage = new StringBuilder(),
                      alertMessage = new StringBuilder();

        if (checkFirst(minedBlock, detecting, debugMessage, alertMessage)) {
            FarLands.getDebugger().echo(debugMessage.toString());
            if (sendAlerts)
                AntiCheat.broadcast(alertMessage.toString(), false);

            mined = 0;
            return;
        }

        finish(minedBlock, minedBlock, detecting);
    }
}
