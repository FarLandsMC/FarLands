package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.util.FLUtils;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvidenceLocker {
    private final Map<String, List<ItemStack>> lockers;

    public EvidenceLocker(OfflineFLPlayer flp) {
        this.lockers = new HashMap<>();
        flp.punishments.forEach(punishment -> lockers.put(punishment.toUniqueString(), new ArrayList<>()));
    }

    public EvidenceLocker(NBTTagCompound nbt) {
        this.lockers = new HashMap<>();
        for (String key : nbt.getKeys()) {
            NBTTagList serLocker = nbt.getList(key, 10);
            List<ItemStack> locker = new ArrayList<>();
            serLocker.stream().map(base -> FLUtils.itemStackFromNBT((NBTTagCompound)base))
                    .forEach(locker::add);
            lockers.put(key, locker);
        }
    }

    public List<ItemStack> getSubLocker(Punishment punishment) {
        return lockers.get(punishment.toUniqueString());
    }

    public EvidenceLocker update(OfflineFLPlayer flp) {
        flp.punishments.stream().map(Punishment::toUniqueString).filter(uid -> !lockers.containsKey(uid))
                .forEach(uid -> lockers.put(uid, new ArrayList<>()));
        return this;
    }

    public NBTTagCompound serialize() {
        NBTTagCompound nbt = new NBTTagCompound();
        lockers.forEach((key, locker) -> {
            NBTTagList serLocker = new NBTTagList();
            locker.stream().map(FLUtils::itemStackToNBT).forEach(serLocker::add);
            nbt.set(key, serLocker);
        });
        return nbt;
    }
}
