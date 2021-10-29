package net.farlands.sanctuary.scheduling;

import net.farlands.sanctuary.FarLands;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles plugin scheduling
 */
public class Scheduler extends Thread {
    private final List<TaskBase> tasks;
    private int currentTaskUid;
    private boolean running;

    public Scheduler() {
        this.tasks = new ArrayList<>();
        this.currentTaskUid = 0;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            synchronized (this) {
                tasks.forEach(TaskBase::tick);
                tasks.removeIf(TaskBase::isComplete);
            }
            try {
                sleep(50L - (System.currentTimeMillis() % 50L)); // Account for execution time
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    @Override
    public void interrupt() {
        running = false;
    }

    public synchronized int scheduleSyncDelayedTask(final Runnable task, long delay) {
        tasks.add(new DelayedTask(currentTaskUid ++, () -> Bukkit.getScheduler().runTask(FarLands.getInstance(), task), delay));
        return currentTaskUid - 1;
    }

    public synchronized int scheduleAsyncDelayedTask(final Runnable task, long delay) {
        tasks.add(new DelayedTask(currentTaskUid ++, task, delay));
        return currentTaskUid - 1;
    }

    public synchronized int scheduleSyncRepeatingTask(final Runnable task, long initialDelay, long cycleTime) {
        tasks.add(new RepeatingTask(currentTaskUid ++, () -> Bukkit.getScheduler().runTask(FarLands.getInstance(), task), initialDelay, cycleTime));
        return currentTaskUid - 1;
    }

    public synchronized int scheduleAsyncRepeatingTask(final Runnable task, long initialDelay, long cycleTime) {
        tasks.add(new RepeatingTask(currentTaskUid ++, task, initialDelay, cycleTime));
        return currentTaskUid - 1;
    }

    public synchronized TaskBase getTask(int uid) {
        return tasks.stream().filter(task -> task.getUid() == uid).findAny().orElse(null);
    }

    public synchronized boolean cancelTask(int uid) {
        TaskBase task = getTask(uid);
        return task != null && tasks.remove(task);
    }

    public synchronized boolean resetTask(int uid) {
        TaskBase task = getTask(uid);
        if (task == null)
            return false;
        task.reset();
        return true;
    }

    public synchronized boolean completeTask(int uid) {
        TaskBase task = getTask(uid);
        if (task == null)
            return false;
        task.complete();
        return tasks.remove(task);
    }

    public synchronized long taskTimeRemaining(int uid) {
        TaskBase task = getTask(uid);
        return task == null ? 0 : task.timeRemaining();
    }
}
