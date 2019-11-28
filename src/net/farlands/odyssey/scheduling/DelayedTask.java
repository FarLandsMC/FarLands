package net.farlands.odyssey.scheduling;

public class DelayedTask extends TaskBase {
    private final long delayConst;
    private long delay;

    public DelayedTask(int uid, Runnable task, long delay) {
        super(uid, task);
        this.delayConst = delay;
        this.delay = delay;
    }

    @Override
    protected void update() {
        if(delay <= 0) {
            complete();
            return;
        }
        -- delay;
    }

    @Override
    public void complete() {
        task.run();
        setComplete(true);
    }

    @Override
    public void reset() {
        delay = delayConst;
    }

    @Override
    public long timeRemaining() {
        return delay;
    }
}
