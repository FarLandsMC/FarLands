package net.farlands.sanctuary.mechanic.anticheat;

import com.kicas.rp.util.Materials;
import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;

import static java.lang.Math.abs;

/**
 * Data store for a player's x-ray detection
 */
public class XRayStore {

    private static final List<BlockFace> SIDES = List.of(
        BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP
    );

    // Used in /mined
    public static final List<Material> ORES = FLUtils.join(
        Materials.materialsEndingWith("_ORE"),
        Material.ANCIENT_DEBRIS,
        Material.AMETHYST_BLOCK,
        Material.BUDDING_AMETHYST
    );


    private final String                          playerName;
    private final boolean                         sendAlerts;
    private       int                             strikes;
    private       Location                        last;
    private       int                             mined;
    private final HashMap<Material, Integer>      obtained;
    private final List<Pair<Detecting, Location>> nodes;

    public XRayStore(String playerName, boolean sendAlerts) {
        this.playerName = playerName;
        this.sendAlerts = sendAlerts;
        this.strikes = 0;
        this.last = null;
        this.mined = 0;
        this.obtained = new HashMap<>();
        this.nodes = new ArrayList<>();
    }

    public void onBlockBreak(BlockBreakEvent event) {
        Block minedBlock = event.getBlock();

        // log all mined ores to flush on logout
        if (ORES.contains(minedBlock.getType())) {
            this.obtained.put(minedBlock.getType(), this.obtained.getOrDefault(minedBlock.getType(), 0) + 1);
        }

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
        ++this.mined;
    }

    public void printObtained() {
        if (this.obtained.isEmpty()) {
            return;
        }

        StringBuilder debugMessage = new StringBuilder();
        debugMessage.append(this.playerName).append(" has broken the following blocks this session:\n");

        for (Map.Entry<Material, Integer> entry : this.obtained.entrySet()) {
            debugMessage.append(" ").append(entry.getKey().name().toLowerCase()).append(" * ").append(entry.getValue()).append("\n");
        }

        FarLands.getDebugger().echo(debugMessage.toString().trim());
    }

    public List<Pair<Detecting, Location>> getNodes() {
        return this.nodes;
    }

    private boolean checkFirst(Block block, Detecting detecting, StringBuilder debugMessage, StringBuilder alertMessage) {
        if (this.last != null) {
            return false;
        }

        this.last = block.getLocation();
        debugMessage.append(this.playerName).append(" : ")
            .append(detecting.name().toLowerCase()).append(" : ")
            .append(this.last.getBlockX()).append(" ")
            .append(this.last.getBlockY()).append(" ")
            .append(this.last.getBlockZ()).append(" @ First Node");
        alertMessage.append(this.playerName).append(" has found a vein of ")
            .append(detecting.color).append(detecting.name().toLowerCase())
            .append(ChatColor.RED).append(" @ ")
            .append(this.last.getBlockX()).append(" ")
            .append(this.last.getBlockY()).append(" ")
            .append(this.last.getBlockZ());
        this.nodes.add(0, new Pair<>(detecting, block.getLocation()));
        return true;
    }

    private void finish(Block minedBlock, Block block, Detecting detecting) {
        final int dx = block.getLocation().getBlockX() - this.last.getBlockX(),
            dy = block.getLocation().getBlockY() - this.last.getBlockY(),
            dz = block.getLocation().getBlockZ() - this.last.getBlockZ();
        // ignore blocks that are mined nearby as they are probably from the same vein
        if (abs(dy) <= 2 && abs(dx) <= 2 && abs(dz) <= 2) {
            return;
        }

        this.nodes.add(0, new Pair<>(detecting, block.getLocation()));
        final String oreType = detecting.color + detecting.toString();
        StringBuilder debugMessage = new StringBuilder(),
            alertMessage = new StringBuilder();

        this.last = block.getLocation();
        debugMessage.append(this.playerName).append(" : ")
            .append(detecting.name().toLowerCase()).append(" : ")
            .append(this.last.getBlockX()).append(" ")
            .append(this.last.getBlockY()).append(" ")
            .append(this.last.getBlockZ()).append(" @ ")
            .append(dx).append(" ")
            .append(dy).append(" ")
            .append(dz);
        alertMessage.append(this.playerName).append(" has found a vein of ")
            .append(oreType).append(ChatColor.RED).append(" @ ")
            .append(this.last.getBlockX()).append(" ")
            .append(this.last.getBlockY()).append(" ")
            .append(this.last.getBlockZ());

        // Minimal path is |dy| + 2(|dx|+|dz|) // * 2.1 ( 4 + rather than 2 to account for an imperfect path
        // We also ignore anything below half the minimal path to account for caving
        int a = abs(dx) + abs(dy) + abs(dz),
            b = abs(dy) + (int) Math.ceil(2.1 * (4 + abs(dx) + abs(dz)));
        debugMessage.append("\nmined : ").append(a).append(" ").append(this.mined).append(" ").append(b);

        if ((abs(dy) <= 3 && (abs(dx) <= 2 || abs(dz) <= 2)) || (a >= this.mined || this.mined >= b)) {
            FarLands.getDebugger().echo(debugMessage.toString());
            if (this.sendAlerts) {
                AntiCheat.broadcast(alertMessage.toString(), false);
            }
        } else {
            alertMessage.setLength(0);
            if (ORES.contains(minedBlock.getType())) {
                debugMessage.append("\nStrikes : ").append(++this.strikes);
                alertMessage.append(this.playerName).append(" might be using X-Ray");
            } else {
                debugMessage.append("\nHidden Node");
                alertMessage.append(this.playerName).append(" has found a hidden ").append(oreType);
            }
            FarLands.getDebugger().echo(debugMessage.toString());
            if (this.sendAlerts) {
                AntiCheat.sendDiscordAlert(alertMessage.toString());
                AntiCheat.broadcast(alertMessage.append(ChatColor.RED).append(" @ ")
                                        .append(this.last.getBlockX()).append(" ")
                                        .append(this.last.getBlockY()).append(" ")
                                        .append(this.last.getBlockZ()).toString(), false);
                AntiCheat.promptToSpec(this.playerName);
            }
        }

        this.mined = 0;
    }

    private void onOreExposed(BlockBreakEvent event, Detecting detecting, Block exposedBlock) {
        StringBuilder debugMessage = new StringBuilder(),
            alertMessage = new StringBuilder();

        if (checkFirst(exposedBlock, detecting, debugMessage, alertMessage)) {
            final boolean hidden = !ORES.contains(event.getBlock().getType());
            if (hidden) {
                debugMessage.append("\nHidden Node");
                alertMessage.setLength(0);
                alertMessage.append(this.playerName).append(" has found a hidden ")
                    .append(detecting.color).append(detecting.toString());
            }

            FarLands.getDebugger().echo(debugMessage.toString());
            if (this.sendAlerts) {
                // TODO: re-enable after persistence update
                // FarLands.getDiscordHandler().sendMessage("alerts", alertMessage.toString());
                AntiCheat.broadcast(alertMessage.append(ChatColor.RED).append(" @ ")
                                        .append(this.last.getBlockX()).append(" ")
                                        .append(this.last.getBlockY()).append(" ")
                                        .append(this.last.getBlockZ()).toString(), false);
                if (hidden) {
                    AntiCheat.promptToSpec(this.playerName);
                }
            }

            this.mined = 0;
            return;
        }

        finish(event.getBlock(), exposedBlock, detecting);
    }

    private void onOreMined(Detecting detecting, Block minedBlock) {
        StringBuilder debugMessage = new StringBuilder(),
            alertMessage = new StringBuilder();

        if (checkFirst(minedBlock, detecting, debugMessage, alertMessage)) {
            FarLands.getDebugger().echo(debugMessage.toString());
            if (this.sendAlerts) {
                AntiCheat.broadcast(alertMessage.toString(), false);
            }

            this.mined = 0;
            return;
        }

        finish(minedBlock, minedBlock, detecting);
    }
}
