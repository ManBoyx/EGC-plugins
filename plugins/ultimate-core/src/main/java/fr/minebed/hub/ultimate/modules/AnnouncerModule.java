package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.compat.scheduler.Scheduler;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.Ctx;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Annonces périodiques dans le chat, prises dans {@code announcer.messages} (chaque entrée est une ligne ou une liste de lignes). */
public final class AnnouncerModule extends AbstractModule {

    private Scheduler.Task task;
    private int next;

    public AnnouncerModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "announcer";
    }

    @Override
    public void enable() {
        start();
    }

    @Override
    public void reload() {
        stop();
        start();
    }

    @Override
    protected void onDisable() {
        stop();
    }

    private void start() {
        long seconds = Math.max(10, ctx.config().getLong("announcer.interval-seconds", 300));
        task = ctx.scheduler.runGlobalTimer(new Runnable() {
            @Override
            public void run() {
                announce();
            }
        }, seconds * 20, seconds * 20);
    }

    private void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void announce() {
        List<?> entries = ctx.config().getList("announcer.messages");
        if (entries == null || entries.isEmpty() || Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        Object entry = entries.get(next++ % entries.size());
        String prefix = ctx.config().getString("announcer.prefix", "");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (entry instanceof List) {
                for (Object line : (List<?>) entry) {
                    p.sendMessage(ctx.messages.format(prefix + line));
                }
            } else {
                p.sendMessage(ctx.messages.format(prefix + entry));
            }
        }
    }
}
