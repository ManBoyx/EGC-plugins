package fr.minebed.hub.compat.scheduler;

import fr.minebed.hub.nms.Reflect;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Planificateur régionalisé de Folia. Ses classes n'existent pas dans l'API Spigot 1.8.8 avec laquelle on compile :
 * tout passe par la réflexion, résolue une fois au démarrage. {@link #create} renvoie {@code null} si Folia est absent.
 */
public final class FoliaScheduler implements Scheduler {

    private final Plugin plugin;
    private final Object global;
    private final Object region;
    private final Object async;
    private final Method globalRun;
    private final Method globalRunDelayed;
    private final Method globalRunAtFixedRate;
    private final Method globalCancel;
    private final Method regionRun;
    private final Method asyncRunNow;
    private final Method asyncCancel;
    private final Method entityGetScheduler;
    private final Method entityRun;
    private final Method entityRunDelayed;
    private final Method taskCancel;
    private final Method taskIsCancelled;

    private FoliaScheduler(Plugin plugin, Object global, Object region, Object async, Method[] m) {
        this.plugin = plugin;
        this.global = global;
        this.region = region;
        this.async = async;
        this.globalRun = m[0];
        this.globalRunDelayed = m[1];
        this.globalRunAtFixedRate = m[2];
        this.globalCancel = m[3];
        this.regionRun = m[4];
        this.asyncRunNow = m[5];
        this.asyncCancel = m[6];
        this.entityGetScheduler = m[7];
        this.entityRun = m[8];
        this.entityRunDelayed = m[9];
        this.taskCancel = m[10];
        this.taskIsCancelled = m[11];
    }

    public static FoliaScheduler create(Plugin plugin) {
        try {
            Class<?> server = Bukkit.getServer().getClass();
            Object global = Reflect.invoke(Reflect.findMethod(server, "getGlobalRegionScheduler"), Bukkit.getServer());
            Object region = Reflect.invoke(Reflect.findMethod(server, "getRegionScheduler"), Bukkit.getServer());
            Object async = Reflect.invoke(Reflect.findMethod(server, "getAsyncScheduler"), Bukkit.getServer());
            if (global == null || region == null || async == null) {
                return null;
            }
            Class<?> globalType = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class<?> regionType = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class<?> asyncType = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            Class<?> entityType = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            Class<?> taskType = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            Method[] m = new Method[12];
            m[0] = globalType.getMethod("run", Plugin.class, Consumer.class);
            m[1] = globalType.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            m[2] = globalType.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            m[3] = globalType.getMethod("cancelTasks", Plugin.class);
            m[4] = regionType.getMethod("run", Plugin.class, Location.class, Consumer.class);
            m[5] = asyncType.getMethod("runNow", Plugin.class, Consumer.class);
            m[6] = asyncType.getMethod("cancelTasks", Plugin.class);
            m[7] = Entity.class.getMethod("getScheduler");
            m[8] = entityType.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
            m[9] = entityType.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
            m[10] = taskType.getMethod("cancel");
            m[11] = taskType.getMethod("isCancelled");
            return new FoliaScheduler(plugin, global, region, async, m);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private final class Handle implements Task {
        private final Object scheduled; // ScheduledTask, ou null (entité déjà disparue)

        Handle(Object scheduled) {
            this.scheduled = scheduled;
        }

        @Override
        public void cancel() {
            if (scheduled != null) {
                Reflect.invoke(taskCancel, scheduled);
            }
        }

        @Override
        public boolean isCancelled() {
            return scheduled == null || Boolean.TRUE.equals(Reflect.invoke(taskIsCancelled, scheduled));
        }
    }

    /** Le {@code Consumer<ScheduledTask>} attendu par Folia, autour d'un simple Runnable. */
    private static Consumer<Object> consumer(final Runnable task) {
        return new Consumer<Object>() {
            @Override
            public void accept(Object scheduledTask) {
                task.run();
            }
        };
    }

    @Override
    public Task runGlobal(Runnable task) {
        return new Handle(Reflect.invoke(globalRun, global, plugin, consumer(task)));
    }

    @Override
    public Task runGlobalLater(Runnable task, long ticks) {
        return new Handle(Reflect.invoke(globalRunDelayed, global, plugin, consumer(task), Math.max(1L, ticks)));
    }

    /** Le Runnable est rappelé à chaque période ; Folia exige un délai et une période d'au moins 1 tick. */
    @Override
    public Task runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        return new Handle(Reflect.invoke(globalRunAtFixedRate, global, plugin, consumer(task), Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public Task runForEntity(Entity entity, Runnable task, Runnable retired) {
        Object scheduler = Reflect.invoke(entityGetScheduler, entity);
        return new Handle(scheduler == null ? null : Reflect.invoke(entityRun, scheduler, plugin, consumer(task), retired));
    }

    @Override
    public Task runForEntityLater(Entity entity, Runnable task, Runnable retired, long ticks) {
        Object scheduler = Reflect.invoke(entityGetScheduler, entity);
        return new Handle(scheduler == null ? null : Reflect.invoke(entityRunDelayed, scheduler, plugin, consumer(task), retired, Math.max(1L, ticks)));
    }

    @Override
    public Task runAtLocation(Location location, Runnable task) {
        return new Handle(Reflect.invoke(regionRun, region, plugin, location, consumer(task)));
    }

    @Override
    public Task runAsync(Runnable task) {
        return new Handle(Reflect.invoke(asyncRunNow, async, plugin, consumer(task)));
    }

    @Override
    public void cancelAll() {
        Reflect.invoke(globalCancel, global, plugin);
        Reflect.invoke(asyncCancel, async, plugin);
    }
}
