package net.farlands.sanctuary.util;

import net.farlands.sanctuary.FarLands;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A set which will store a value for a certain amount of time before removing it.
 * <p>
 * When a value is added the first time, it will set the delay to value for this map, if it is added again before the
 * delay runs out, the time is reset.
 */
public class TimedSet<T> implements Set<T> {

    // Note: Since the keys in a map are a set, we can use just a map, no need for another set.
    private final Map<T, BukkitTask> tasks;
    private final long               delay;

    /**
     * Create a timed set using a {@link HashSet}
     *
     * @param delay the delay (ticks) before values are removed from the set
     */
    public TimedSet(long delay) {
        this.tasks = new HashMap<>();
        this.delay = delay;
    }

    /**
     * Add an item to the set, resetting its delay if it's already in the set.
     * <p>
     * **Important note:** While traditional sets don't modify the set if `t` is already in the set, this will reset its
     * delay
     * <p>
     * It still returns false if the value is in the set and true if it was added to match with the interface.
     */
    public boolean add(T t) {
        BukkitTask existing = this.tasks.get(t);
        if (existing != null) {
            existing.cancel();
        }
        this.tasks.put(t, Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> this.tasks.remove(t), this.delay));
        return existing == null;
    }

    /**
     * remove an item from the set, no matter what delay it still has left
     */
    public boolean remove(Object o) {
        BukkitTask existing = this.tasks.get(o);
        if (existing != null) {
            existing.cancel();
            return true;
        }
        return false;
    }

    public boolean addAll(@NotNull Collection<? extends T> collection) {
        collection.forEach(this::add);
        return true;
    }

    public boolean retainAll(@NotNull Collection<?> collection) {
        boolean changed = false;
        for (T k : this.tasks.keySet()) {
            if (!collection.contains(k)) {
                this.remove(k);
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(@NotNull Collection<?> collection) {
        boolean changed = false;
        for (Object o : collection) {
            changed |= this.remove(o);
        }
        return changed;
    }

    public void clear() {
        for (T k : this.tasks.keySet()) {
            this.remove(k);
        }
    }

    /**
     * Get the amount of values currently in this set -- Note: this is only values which still have time left until
     * they're removed
     */
    public int size() {
        return this.tasks.size();
    }

    /**
     * Check if there are no pending items to be removed from the set
     */
    public boolean isEmpty() {
        return this.tasks.isEmpty();
    }

    /**
     * Check if an item is still pending to be removed
     */
    public boolean contains(Object o) {
        return this.tasks.containsKey(o);
    }

    /**
     * iterator over pending items
     */
    public Iterator<T> iterator() {
        return this.tasks.keySet().iterator();
    }

    /**
     * Get an array of pending items
     */
    public Object[] toArray() {
        return this.tasks.keySet().toArray();
    }

    /**
     * Get an array of pending items
     */
    public <U> U[] toArray(@NotNull U[] us) {
        return this.tasks.keySet().toArray(us);
    }

    public boolean containsAll(@NotNull Collection<?> collection) {
        return this.tasks.keySet().containsAll(collection);
    }
}
