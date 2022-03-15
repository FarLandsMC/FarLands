package net.farlands.sanctuary.data;

import net.farlands.sanctuary.FarLands;

/**
 * A field that contains a temporary value that only lasts for a certain amount of time
 * @param <T> object
 */
public class TimedField<T> {
    private T value;
    private int expirationTaskUid;

    public TimedField() {
        this.value = null;
        this.expirationTaskUid = -1;
    }

    public synchronized T getValue() {
        return value;
    }

    public synchronized boolean isSet() {
        return value != null;
    }

    public void discard() {
        setValue(null);
        FarLands.getScheduler().cancelTask(expirationTaskUid);
    }

    public void setValue(T value, long expirationDelay, Runnable onExpire) {
        setValue(value);
        FarLands.getScheduler().cancelTask(expirationTaskUid);
        expirationTaskUid = FarLands.getScheduler().scheduleAsyncDelayedTask(() -> {
            setValue(null);
            if (onExpire != null)
                onExpire.run();
        }, expirationDelay);
    }

    private synchronized void setValue(T value) {
        this.value = value;
    }
}
