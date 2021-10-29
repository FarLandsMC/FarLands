package net.farlands.sanctuary.scheduling;

/**
 * Represents a task that runs multiple times.
 */
public class RepeatingTask extends TaskBase {
    private final long cycleTime;
    private long delay;
    private boolean initialDelayComplete;

    public RepeatingTask(int uid, Runnable task, long initialDelay, long cycleTime) {
        super(uid, task);
        this.cycleTime = cycleTime;
        this.initialDelayComplete = initialDelay <= 0;
        this.delay = initialDelayComplete ? 0 : initialDelay;
    }

    @Override
    protected void update() {
        if (delay <= 0) {
            if (initialDelayComplete) {
                task.run();
                delay = cycleTime;
            } else
                initialDelayComplete = true;
        } else
            -- delay;
    }

    @Override
    public void complete() {
        task.run();
        setComplete(true);
    }

    @Override
    public void reset() {
        delay = cycleTime;
    }

    @Override
    public long timeRemaining() {
        return delay;
    }
}
