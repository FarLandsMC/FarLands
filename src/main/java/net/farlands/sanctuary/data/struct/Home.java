package net.farlands.sanctuary.data.struct;

import com.google.common.collect.ImmutableMap;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.LocationWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;

/**
 * A player home.
 */
public final class Home extends LocationWrapper {
    private static Map<String, TextColor> WORLD_COLORS = new ImmutableMap.Builder < String, TextColor>()
        .put("world", NamedTextColor.GREEN)
        .put("world_nether", NamedTextColor.RED)
        .put("world_the_end", NamedTextColor.YELLOW) // not possible, but here for completion :P
        .put("farlands", NamedTextColor.DARK_GREEN)
        .build();


    private String name;

    public Home(String name, UUID world, double x, double y, double z, float yaw, float pitch) {
        super(world, x, y, z, yaw, pitch);
        this.name = name;
    }

    public Home(String name, Location loc) {
        this(name, loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public Location getLocation() {
        return asLocation();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param colorDimension Color the home name with the dimension color
     * @param clickTp Click on a home to teleport to it?
     * @return component representation of this home.
     */
    public Component asComponent(boolean colorDimension, boolean clickTp) {
        Component nameC = Component.text(this.name + ": ").color(colorDimension ? WORLD_COLORS.get(asLocation().getWorld().getName()) : NamedTextColor.GOLD);
        Component coords = ComponentColor.aqua("%d %d %d", (int) x, (int) y, (int) z);
        Component finalC = nameC.append(Component.space()).append(coords);
        if(clickTp) {
            finalC = ComponentUtils.command("/home " + this.name, finalC, ComponentColor.gold("Teleport to home"));
        }
        return finalC;
    }

    @Override
    public String toString() {
        return ChatColor.GOLD + name + ": " + ChatColor.AQUA + FLUtils.toStringTruncated(x) + ", " +
                FLUtils.toStringTruncated(y) + ", " + FLUtils.toStringTruncated(z);
    }
}
