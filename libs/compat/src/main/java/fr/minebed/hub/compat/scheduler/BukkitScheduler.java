package fr.minebed.hub.compat.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Planificateur classique de Bukkit (Bukkit, Spigot, Paper). */
public final class BukkitScheduler implements Scheduler {

    private final Plugin plugin;

    public BukkitScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    private static final class Handle implements Task {
        private final BukkitTask task;
        private volatile boolean cancelled;

        Handle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            cancelled = true;
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    @Override
    public Task runGlobal(Runnable task) {
        return new Handle(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public Task runGlobalLater(Runnable task, long ticks) {
        return new Handle(Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, ticks)));
    }

    @Override
    public Task runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        return new Handle(Bukkit.getScheduler().runTaskTimer(plugin, task, Math.max(1, delayTicks), Math.max(1, periodTicks)));
    }

    @Override
    public Task runForEntity(Entity entity, Runnable task, Runnable retired) {
        return runGlobal(guarded(entity, task, retired));
    }

    @Override
    public Task runForEntityLater(Entity entity, Runnable task, Runnable retired, long ticks) {
        return runGlobalLater(guarded(entity, task, retired), ticks);
    }

    private static Runnable guarded(final Entity entity, final Runnable task, final Runnable retired) {
        return new Runnable() {
            @Override
            public void run() {
                if (entity.isValid()) {
                    task.run();
                } else if (retired != null) {
                    retired.run();
                }
            }
        };
    }

    @Override
    public Task runAtLocation(Location location, Runnable task) {
        return runGlobal(task);
    }

    @Override
    public Task runAsync(Runnable task) {
        return new Handle(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
