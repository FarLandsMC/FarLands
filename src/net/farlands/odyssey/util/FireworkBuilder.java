package net.farlands.odyssey.util;

import net.minecraft.server.v1_15_R1.EntityFireworks;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftFirework;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FireworkBuilder {
    private final List<Explosion> explosions;
    private final int lifetime;
    private final int flight;
    private int currentExplosion;

    public FireworkBuilder(int lifetime, int flight) {
        this.explosions = new ArrayList<>(4);
        this.lifetime = lifetime;
        this.flight = flight;
        this.currentExplosion = 0;
    }

    public FireworkBuilder() {
        this(20, 2);
    }

    public static FireworkBuilder randomFirework(int lifetime, int flight, int numExplosions) {
        FireworkBuilder builder = new FireworkBuilder(lifetime, flight);
        Random rng = new Random();
        for(int i = 0;i < numExplosions;++ i) {
            builder.addExplosion(FireworkExplosionType.randomType(rng), randomColors(rng.nextInt(3) + 1, rng))
                    .setFlicker(rng.nextBoolean())
                    .setTrail(rng.nextBoolean())
                    .setFadeColors(randomColors(rng.nextInt(4), rng))
                    .add();
        }
        return builder;
    }

    private static int[] randomColors(int numColors, Random random) {
        if(numColors == 0)
            return new int[] {255, 255, 255};
        int[] colors = new int[numColors * 3];
        for(int i = 0;i < numColors;++ i)
            System.arraycopy(Utils.hsv2rgb(360 * random.nextDouble(), random.nextDouble() * 0.5 + 0.5, 1.0), 0, colors, i, 3);
        // (RGB_OPTIONS[random.nextInt(3)] << 16) | (RGB_OPTIONS[random.nextInt(3)] << 8) | RGB_OPTIONS[random.nextInt(3)]
        return colors;
    }

    public FireworkBuilder addExplosion(FireworkExplosionType type, int... colors) {
        explosions.add(new Explosion(type, condenseRGB(colors)));
        return this;
    }

    public FireworkBuilder setFlicker(boolean flicker) {
        explosions.get(currentExplosion).flicker = flicker;
        return this;
    }

    public FireworkBuilder setTrail(boolean trail) {
        explosions.get(currentExplosion).trail = trail;
        return this;
    }

    public FireworkBuilder setFadeColors(int... colors) {
        explosions.get(currentExplosion).fadeColors = condenseRGB(colors);
        return this;
    }

    public FireworkBuilder add() {
        ++ currentExplosion;
        return this;
    }

    private static int[] condenseRGB(int... rgb) {
        int[] colors = new int[rgb.length / 3];
        for(int i = 0;i < rgb.length;i += 3)
            colors[i / 3] = (rgb[i] << 16) | (rgb[i + 1] << 8) | rgb[i + 2];
        return colors;
    }

    private NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInt("LifeTime", lifetime);
        NBTTagCompound fireworksItem = new NBTTagCompound();
        fireworksItem.setString("id", "firework_rocket");
        fireworksItem.setInt("Count", 1);
        NBTTagCompound itemTag = new NBTTagCompound();
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setInt("Flight", flight);
        NBTTagList explosionTags = new NBTTagList();
        explosions.forEach((e) -> explosionTags.add(e.toNBT()));
        wrapper.set("Explosions", explosionTags);
        itemTag.set("Fireworks", wrapper);
        fireworksItem.set("tag", itemTag);
        nbt.set("FireworksItem", fireworksItem);
        return nbt;
    }

    public void spawnEntity(Location loc) {
        Firework firework = (Firework)loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        EntityFireworks entity = ((CraftFirework)firework).getHandle();
        entity.a(toNBT());
    }

    public ItemStack buildItemStack(int stackSize) {
        net.minecraft.server.v1_15_R1.ItemStack stack = CraftItemStack.asNMSCopy(new ItemStack(Material.FIREWORK_ROCKET, stackSize));
        NBTTagCompound stackTag = toNBT().getCompound("FireworksItem").getCompound("tag");
        int flightRaw = (int)Math.ceil(((double)lifetime) / 20.0);
        int flight = flightRaw < 1 ? 1 : Math.min(flightRaw, 3);
        stackTag.getCompound("Fireworks").setByte("Flight", (byte)(flight & 0xFF));
        stack.setTag(stackTag);
        return CraftItemStack.asBukkitCopy(stack);
    }

    private static class Explosion {
        FireworkExplosionType type;
        boolean flicker;
        boolean trail;
        int[] colors;
        int[] fadeColors;

        Explosion(FireworkExplosionType type, int... colors) {
            this.type = type;
            this.flicker = false;
            this.trail = false;
            this.colors = colors;
            this.fadeColors = new int[] {255, 255, 255};
        }

        NBTTagCompound toNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInt("Type", type.ordinal());
            nbt.setBoolean("Flicker", flicker);
            nbt.setBoolean("Trail", trail);
            nbt.setIntArray("Colors", colors);
            nbt.setIntArray("FadeColors", fadeColors);
            return nbt;
        }
    }
}
