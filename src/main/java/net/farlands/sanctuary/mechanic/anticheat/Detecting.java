package net.farlands.sanctuary.mechanic.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Different things to detect for cheating.
 */
public enum Detecting {

    ANCIENT_DEBRIS (new String[]{"world_nether"},      128, ChatColor.LIGHT_PURPLE, Material.ANCIENT_DEBRIS),
    DIAMOND        (new String[]{"world", "farlands"}, 16,  ChatColor.AQUA,         Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
    EMERALD        (new String[]{"world", "farlands"}, 32,  ChatColor.GREEN,        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);
    //AMETHYST       (new String[]{"world", "farlands"}, 75,  ChatColor.DARK_PURPLE,  Material.AMETHYST_BLOCK, Material.CALCITE, Material.BUDDING_AMETHYST);

    String[]   worldNames;
    int        maxYSpawn;
    Material[] materials;
    ChatColor  color;

    Detecting(String[] worldNames, int maxYSpawn, ChatColor color, Material... materials) {
        this.worldNames = worldNames;
        this.maxYSpawn  = maxYSpawn;
        this.color      = color;
        this.materials  = materials;
    }

    public boolean isValid(Block block) {
        if (Arrays.stream(worldNames).noneMatch(s -> s.equals(block.getWorld().getName())))
            return false;
        if (Arrays.stream(materials).noneMatch(b -> b == block.getType()))
            return false;
        if (maxYSpawn < block.getLocation().getBlockY())
            return false;

        return true;
    }

    public ChatColor getColor() {
        return color;
    }


    @Override
    public String toString() {
        return Arrays.stream(materials).map(Enum::name).map(String::toLowerCase).collect(Collectors.joining(", "));
    }
}