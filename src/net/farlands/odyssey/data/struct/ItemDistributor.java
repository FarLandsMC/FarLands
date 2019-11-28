package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.LocationWrapper;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

public class ItemDistributor {
    private LocationWrapper source;
    private LocationWrapper pub;
    private LocationWrapper priv;
    private transient int count;

    public ItemDistributor(Location source, Location pub, Location priv) {
        this.source = new LocationWrapper(source);
        this.pub = new LocationWrapper(pub);
        this.priv = new LocationWrapper(priv);
        this.count = 0;
    }

    public ItemDistributor() {
        this.source = Utils.LOC_ZERO;
        this.pub = Utils.LOC_ZERO;
        this.priv = Utils.LOC_ZERO;
        this.count = 0;
    }

    public boolean hasSourceAt(Location location) {
        return source.asLocation().equals(location);
    }

    public void setSource(Location source) {
        this.source = new LocationWrapper(source);
    }

    public void setPublic(Location pub) {
        this.pub = new LocationWrapper(pub);
    }

    public void setPrivate(Location priv) {
        this.priv = new LocationWrapper(priv);
    }

    public void accept(InventoryMoveItemEvent event) {
        World world = ((CraftWorld)source.asLocation().getWorld()).getHandle();
        Inventory sourceInv = world.getTileEntity(source.asBlockPosition()).getOwner().getInventory(),
                pubInv = world.getTileEntity(pub.asBlockPosition()).getOwner().getInventory(),
                privInv = world.getTileEntity(priv.asBlockPosition()).getOwner().getInventory();
        if(source.asLocation().equals(event.getDestination().getLocation()) && sourceInv != null && pubInv != null && privInv != null) {
            Inventory to = count % FarLands.getFLConfig().getTotalItems() < FarLands.getFLConfig().getPublicItems() ? pubInv : privInv;
            to.addItem(event.getItem().clone());
            ++ count;
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> sourceInv.clear());
        }
    }
}
