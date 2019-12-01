package net.farlands.odyssey.data;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.Utils;

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
        reset(Utils.ACTION_NULL);
    }

    public void resetCurrentTask() {
        FarLands.getScheduler().resetTask(taskUid);
    }

    public void cancel() {
        FarLands.getScheduler().cancelTask(taskUid);
    }
}
