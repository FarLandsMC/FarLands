package net.farlands.sanctuary.util;

import java.util.Random;

/**
 * Types of firework explosions.
 */
public enum FireworkExplosionType {
    SMALL_BALL,
    LARGE_BALL,
    STAR,
    CREEPER,
    BURST;

    public static final FireworkExplosionType[] VALUES = values();

    public static FireworkExplosionType randomType(Random random) {
        return VALUES[random.nextInt(VALUES.length)];
    }
}