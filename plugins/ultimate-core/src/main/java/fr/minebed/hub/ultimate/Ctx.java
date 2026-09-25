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

    /** Le joueur en ligne dont le pseudo est exactement {@code name} (majuscules ignorées) ; pour l'argent, jamais d'approximation. */
    public Player findOnlineExact(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /** Comme {@link #findOnlineExact}, mais accepte aussi le début d'un pseudo s'il n'y a qu'un joueur qui y correspond (commandes de confort). */
    public Player findOnline(String name) {
        Player exact = findOnlineExact(name);
        if (exact != null || name == null || name.isEmpty()) {
            return exact;
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        Player match = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(java.util.Locale.ROOT).startsWith(lower)) {
                if (match != null) {
                    return null; // ambigu
                }
                match = p;
            }
        }
        return match;
    }

    /** Prévient les membres du staff en ligne (permission « ultimatecore.staff.notify ») et la console. */
    public void notifyStaff(String text) {
        String line = messages.raw(Msg.PREFIX) + messages.raw(Msg.STAFF_NOTIFY, "message", text);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("ultimatecore.staff.notify")) {
                p.sendMessage(line);
            }
        }
        Bukkit.getConsoleSender().sendMessage(line);
    }

    public boolean moduleEnabled(String id) {
        return config().getBoolean("modules." + id, true);
    }
}
