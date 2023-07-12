package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.LocationWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.UUID;

/**
 * A player home.
 */
public final class Home extends LocationWrapper {

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
        Component nameC = Component.text(this.name + ": ").color(colorDimension ? FLUtils.WORLD_COLORS.get(Worlds.getByWorld(asLocation().getWorld())) : NamedTextColor.GOLD);
        Component coords = ComponentColor.aqua("{} {} {}", (int) x, (int) y, (int) z);
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
