package net.farlands.sanctuary.data;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.FLUtils;

/**
 * Class to determine if a given amount of time has passed in ticks
 */
public class Cooldown {
    private final long delay;
    private int taskUid;

    public Cooldown(long delay) {
        this.delay = delay;
        this.taskUid = -1;
    }

    public boolean isComplete() {
        return timeRemaining() == 0;
    }

    public long timeRemaining() {
        return FarLands.getScheduler().taskTimeRemaining(taskUid);
    }

    public void reset(Runnable task) {
        FarLands.getScheduler().cancelTask(taskUid);
        taskUid = FarLands.getScheduler().scheduleSyncDelayedTask(task, delay);
    }

    public void reset() {
        reset(FLUtils.NO_ACTION);
    }

    public void resetCurrentTask() {
        FarLands.getScheduler().resetTask(taskUid);
    }

    public void cancel() {
        FarLands.getScheduler().cancelTask(taskUid);
    }
}
