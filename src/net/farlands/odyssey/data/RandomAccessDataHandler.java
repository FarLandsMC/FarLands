package net.farlands.odyssey.data;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.scheduling.TaskBase;
import net.farlands.odyssey.util.Utils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores transient values that could be modified at any time on any interval.
 */
public class RandomAccessDataHandler {
    private final Map<DataKey, Object> dataStore;
    private final Map<DataKey, Integer> cooldowns;

    RandomAccessDataHandler() {
        this.dataStore = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        FarLands.getDebugger().post("radh.data", (unused) -> {
            StringBuilder sb = new StringBuilder();
            dataStore.forEach((key, val) -> sb.append(key).append(": ").append(val).append('\n'));
            return sb.toString();
        });
        FarLands.getDebugger().post("radh.cooldowns", (unused) -> {
            StringBuilder sb = new StringBuilder();
            cooldowns.forEach((key, val) -> sb.append(key).append(": ").append(val).append('\n'));
            return sb.toString();
        });
        FarLands.log("Initialized randomDouble access data handler.");
    }

    public void setCooldown(long delay, String category, String uid, Runnable onFinish) {
        cooldowns.put(new DataKey(category, uid), FarLands.getScheduler().scheduleSyncDelayedTask(onFinish == null ? Utils.ACTION_NULL : onFinish, delay));
    }

    public void setCooldown(long delay, String category, String uid) {
        if(delay <= 0L)
            return;
        setCooldown(delay, category, uid, Utils.ACTION_NULL);
    }

    public void resetCooldown(String category, String uid) {
        TaskBase cd = getCooldown(category, uid);
        if(cd != null)
            cd.reset();
    }

    public void resetOrSetCooldown(long delay, String category, String uid, Runnable onFinish) {
        TaskBase cd = getCooldown(category, uid);
        if(cd != null)
            cd.reset();
        else
            setCooldown(delay, category, uid, onFinish);
    }

    public long cooldownTimeRemaining(String category, String uid) {
        TaskBase cd = getCooldown(category, uid);
        return cd == null ? 0L : cd.timeRemaining();
    }

    public boolean isCooldownComplete(String category, String uid) {
        return cooldownTimeRemaining(category, uid) <= 0;
    }

    public void removeCooldown(String category, String uid) {
        DataKey dk = new DataKey(category, uid);
        if(cooldowns.containsKey(dk))
            FarLands.getScheduler().cancelTask(cooldowns.remove(dk));
    }

    private TaskBase getCooldown(String category, String uid) {
        DataKey dk = new DataKey(category, uid);
        Integer cd = cooldowns.get(dk);
        if(cd == null)
            return null;
        TaskBase task = FarLands.getScheduler().getTask(cd);
        if(task == null) {
            cooldowns.remove(dk);
            return null;
        }
        return task;
    }

    public void store(Object x, String category, String uid) {
        if(x != null)
            dataStore.put(new DataKey(category, uid), x);
    }

    public boolean flipBoolean(String category, String uid) {
        boolean newValue = !retrieveBoolean(category, uid);
        store(newValue, category, uid);
        return newValue;
    }

    public boolean flipBoolean(boolean def, String category, String uid) {
        boolean newValue = !retrieveBoolean(def, category, uid);
        store(newValue, category, uid);
        return newValue;
    }

    public Object retrieve(String category, String uid) {
        return dataStore.get(new DataKey(category, uid));
    }

    public Object retrieveAndStore(Object obj, String category, String uid) {
        Object old = retrieve(category, uid);
        store(obj, category, uid);
        return old;
    }

    public Object retrieveAndStoreIfAbsent(Object obj, String category, String uid) {
        Object value = retrieve(category, uid);
        if(value == null) {
            store(obj, category, uid);
            return obj;
        }else
            return value;
    }

    public int retrieveInt(String category, String uid) {
        return (int)retrieve(category, uid);
    }

    public long retrieveLong(String category, String uid) {
        return (long)retrieve(category, uid);
    }

    public double retrieveDouble(String category, String uid) {
        return (double)retrieve(category, uid);
    }

    public String retrieveString(String category, String uid) {
        return (String)retrieve(category, uid);
    }

    public boolean retrieveBoolean(String category, String uid) {
        Object o = retrieve(category, uid);
        return o != null && (boolean)o;
    }

    public boolean retrieveBoolean(boolean def, String category, String uid) {
        Object o = retrieve(category, uid);
        return o == null ? def : (boolean)o;
    }

    public void delete(String category, String uid) {
        dataStore.remove(new DataKey(category, uid));
    }

    // TODO: If this class is useful elsewhere, place it in the data package
    private static class DataKey {
        private final String category;
        private final String uid;
        private final int hash;

        public DataKey(String category, String uid) {
            this.category = category;
            this.uid = uid;
            this.hash = genHashCode(); // Make the hash beforehand for speed
        }

        public long getHash() {
            return hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other != null && DataKey.class.equals(other.getClass()) && hash == ((DataKey)other).getHash();
        }

        @Override
        public String toString() {
            return category + ":" + uid;
        }

        private int genHashCode() {
            int h = 3;
            h = 13 * h + Objects.hashCode(category);
            h = 13 * h + Objects.hashCode(uid);
            return h;
        }
    }
}
