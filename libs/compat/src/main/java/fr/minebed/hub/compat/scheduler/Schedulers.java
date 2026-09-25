package fr.minebed.hub.compat.scheduler;

import fr.minebed.hub.compat.Platform;
import org.bukkit.plugin.Plugin;

/** Choisit le bon planificateur pour la plateforme. */
public final class Schedulers {

    private Schedulers() {
    }

    public static Scheduler forPlugin(Plugin plugin) {
        if (Platform.current().isFolia()) {
            FoliaScheduler folia = FoliaScheduler.create(plugin);
            if (folia != null) {
                return folia;
            }
            plugin.getLogger().warning("Folia détecté mais son planificateur est inutilisable : repli sur le planificateur classique (risque d'erreurs).");
        }
        return new BukkitScheduler(plugin);
    }
}
