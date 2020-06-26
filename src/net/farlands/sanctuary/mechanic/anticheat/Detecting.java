package net.farlands.sanctuary.mechanic.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

public enum Detecting {

    ANCIENT_DEBRIS("nether", 128, Material.ANCIENT_DEBRIS, ChatColor.LIGHT_PURPLE),
    DIAMOND("world", 16, Material.DIAMOND_ORE, ChatColor.AQUA),
    EMERALD("world", 32, Material.EMERALD_ORE, ChatColor.GREEN);

    String    worldName;
    int       maxYSpawn;
    Material material;
    ChatColor color;

    Detecting(String worldName, int maxYSpawn, Material material, ChatColor color) {
        this.worldName = worldName;
        this.maxYSpawn = maxYSpawn;
        this.material  = material;
        this.color     = color;
    }

    public boolean isValid(Block block) {
        if (!worldName.equals(block.getWorld().getName()))
            return false;
        if (material != block.getType())
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
        return material.name().toLowerCase();
    }
}