package fr.minebed.hub.compat;

import fr.minebed.hub.compat.scheduler.Scheduler;
import fr.minebed.hub.nms.Reflect;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Téléportation sûre. Sur Paper (1.13+) et Folia on utilise {@code teleportAsync} (obligatoire sur Folia, où {@code teleport}
 * est interdit hors de la région de l'entité) ; ailleurs, {@code teleport} depuis le bon fil.
 */
public final class Teleports {

    private static final Method TELEPORT_ASYNC = Reflect.findMethod(org.bukkit.entity.Entity.class, "teleportAsync", Location.class);

    private Teleports() {
    }

    public static boolean hasAsyncTeleport() {
        return TELEPORT_ASYNC != null;
    }

    /** @param done reçoit {@code true} si le joueur a été téléporté (peut être {@code null}) */
    @SuppressWarnings("unchecked")
    public static void teleport(final Scheduler scheduler, final Player player, final Location target, final Consumer<Boolean> done) {
        if (TELEPORT_ASYNC != null) {
            try {
                Object future = TELEPORT_ASYNC.invoke(player, target);
                if (future instanceof CompletableFuture) {
                    ((CompletableFuture<Boolean>) future).whenComplete((ok, error) -> {
                        if (done != null) {
                            done.accept(error == null && Boolean.TRUE.equals(ok));
                        }
                    });
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                // On retombe sur la téléportation classique ci-dessous.
            }
        }
        scheduler.runForEntity(player, new Runnable() {
            @Override
            public void run() {
                boolean ok = player.teleport(target);
                if (done != null) {
                    done.accept(ok);
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (done != null) {
                    done.accept(false);
                }
            }
        });
    }
}
