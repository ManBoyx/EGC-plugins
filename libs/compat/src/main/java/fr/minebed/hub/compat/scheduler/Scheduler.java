package fr.minebed.hub.compat.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Planification indépendante de la plateforme. Sur Bukkit/Spigot/Paper tout se passe sur le fil principal ; sur Folia le
 * monde est découpé en régions et chaque tâche doit s'exécuter dans la région de ce qu'elle touche : d'où ces variantes.
 */
public interface Scheduler {

    /** Une tâche planifiée, que l'on peut annuler. */
    interface Task {
        void cancel();

        boolean isCancelled();
    }

    /** Fil principal (ou région « globale » sur Folia). */
    Task runGlobal(Runnable task);

    Task runGlobalLater(Runnable task, long ticks);

    Task runGlobalTimer(Runnable task, long delayTicks, long periodTicks);

    /** Dans la région qui gère cette entité ; {@code retired} est appelé si l'entité a disparu entre-temps (peut être {@code null}). */
    Task runForEntity(Entity entity, Runnable task, Runnable retired);

    Task runForEntityLater(Entity entity, Runnable task, Runnable retired, long ticks);

    Task runAtLocation(Location location, Runnable task);

    /** Hors du fil principal : réservé aux entrées/sorties (fichiers, réseau), jamais à l'API du jeu. */
    Task runAsync(Runnable task);

    /** Annule tout ce que ce plugin a planifié (à l'arrêt). */
    void cancelAll();
}
