package net.farlands.odyssey.scheduling;

public abstract class TaskBase {
    protected final int uid;
    protected final Runnable task;
    private boolean complete;

    protected TaskBase(int uid, Runnable task) {
        this.uid = uid;
        this.task = task;
        this.complete = false;
    }

    public final int getUid() {
        return uid;
    }

    public final boolean isComplete() {
        return complete;
    }

    public final void setComplete(boolean complete) {
        this.complete = complete;
    }

    protected abstract void update();

    public abstract void complete();

    public abstract void reset();

    public abstract long timeRemaining();

    public final void tick() {
        if(complete)
            return;
        update();
    }
}
