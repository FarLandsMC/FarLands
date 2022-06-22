package net.farlands.sanctuary.mechanic.anticheat;

import net.farlands.sanctuary.data.Worlds;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * X-Ray information for detection
 */
public enum Detecting {

    ANCIENT_DEBRIS (new Worlds[]{ Worlds.NETHER                   }, 128, LIGHT_PURPLE, Material.ANCIENT_DEBRIS                                              ),
    DIAMOND        (new Worlds[]{ Worlds.OVERWORLD, Worlds.POCKET }, 16,  AQUA, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE                         ),
    EMERALD        (new Worlds[]{ Worlds.OVERWORLD, Worlds.POCKET }, 32,  GREEN, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE                        ),
//    AMETHYST       (new Worlds[]{ Worlds.OVERWORLD, Worlds.POCKET }, 75,  DARK_PURPLE,  Material.AMETHYST_BLOCK, Material.CALCITE, Material.BUDDING_AMETHYST ),
    ;

    final Worlds[]       worldNames;
    final int            maxYSpawn;
    final Material[]     materials;
    final NamedTextColor color;

    /**
     * @param worldNames Names of worlds in which this detection applies to
     * @param maxYSpawn  Max Y value of the spawning
     * @param color      Color of the detection
     * @param materials  Materials that this detection applies to
     */
    Detecting(Worlds[] worldNames, @Range(from = 0, to = 320) int maxYSpawn, NamedTextColor color, Material... materials) {
        this.worldNames = worldNames;
        this.maxYSpawn = maxYSpawn;
        this.color = color;
        this.materials = materials;
    }

    /**
     * Check if this detection applies to the provided block
     */
    public boolean isValid(Block block) {
        if (Arrays.stream(this.worldNames).noneMatch(s -> s.matches(block.getWorld()))) {
            return false;
        }
        if (Arrays.stream(this.materials).noneMatch(b -> b == block.getType())) {
            return false;
        }
        return this.maxYSpawn >= block.getLocation().getBlockY();
    }

    /**
     * Get the color used for this detection
     */
    public NamedTextColor color() {
        return this.color;
    }

    @Override
    public String toString() {
        return Arrays.stream(this.materials).map(Enum::name).map(String::toLowerCase).collect(Collectors.joining(", "));
    }
}