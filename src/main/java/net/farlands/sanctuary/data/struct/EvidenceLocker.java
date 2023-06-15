package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.nbt.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * A player evidence locker for storing items when a player has been punished.
 */
public record EvidenceLocker(Map<String, List<ItemStack>> lockers) {

    public EvidenceLocker(OfflineFLPlayer flp) {
        this(new HashMap<>());
        flp.punishments.forEach(punishment -> lockers.put(punishment.toUniqueString(), new ArrayList<>()));
    }

    public EvidenceLocker(CompoundBinaryTag nbt) {
        this(new HashMap<>());
        for (String key : nbt.keySet()) {
            ListBinaryTag serLocker = nbt.getList(key);
            List<ItemStack> locker = new ArrayList<>();
            serLocker
                .stream()
                .map(base -> FLUtils.itemStackFromNBT(((ByteArrayBinaryTag) base).value()))
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

    public CompoundBinaryTag serialize() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        lockers.forEach((key, locker) -> {
            ListBinaryTag serLocker = ListBinaryTag.from(locker.stream().filter(Objects::nonNull).map(FLUtils::itemStackToNBT).map(ByteArrayBinaryTag::of).toList());
            nbt.put(key, serLocker);
        });
        return nbt.build();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (EvidenceLocker) obj;
        return Objects.equals(this.lockers, that.lockers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lockers);
    }

    @Override
    public String toString() {
        return "EvidenceLocker[" +
               "lockers=" + lockers + ']';
    }

}
