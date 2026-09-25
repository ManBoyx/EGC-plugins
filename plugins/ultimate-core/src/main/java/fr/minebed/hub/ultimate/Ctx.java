package fr.minebed.hub.ultimate;

import fr.minebed.hub.compat.Platform;
import fr.minebed.hub.compat.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Ce que les modules partagent : le plugin, le planificateur, les messages, la plateforme. */
public final class Ctx {

    public final JavaPlugin plugin;
    public final Platform platform;
    public final Scheduler scheduler;
    public final Messages messages;

    public Ctx(JavaPlugin plugin, Platform platform, Scheduler scheduler, Messages messages) {
        this.plugin = plugin;
        this.platform = platform;
        this.scheduler = scheduler;
        this.messages = messages;
    }

    public FileConfiguration config() {
        return plugin.getConfig();
    }

    /**
     * Exécute quelque chose sur un joueur depuis n'importe quel fil. Sur Bukkit/Spigot/Paper c'est tout de suite (on est
     * déjà sur le fil principal) ; sur Folia il faut passer par la région du joueur.
     */
    public void runOnPlayer(Player player, Runnable task) {
        if (platform.isFolia()) {
            scheduler.runForEntity(player, task, null);
        } else {
            task.run();
        }
    }

    /** Le joueur en ligne dont le pseudo est exactement {@code name} (majuscules ignorées), sinon celui dont il est le début unique. */
    public Player findOnline(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        Player match = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
            if (p.getName().toLowerCase(java.util.Locale.ROOT).startsWith(name.toLowerCase(java.util.Locale.ROOT))) {
                if (match != null) {
                    return null; // ambigu
                }
                match = p;
            }
        }
        return match;
    }

    public boolean moduleEnabled(String id) {
        return config().getBoolean("modules." + id, true);
    }
}
