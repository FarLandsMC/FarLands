package net.farlands.sanctuary.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.World;

public class NMSUtils {

    public static WorldServer getWorldServer(MinecraftServer server, ResourceKey<World> dimension) {
        return server.a(dimension);
    }

}
