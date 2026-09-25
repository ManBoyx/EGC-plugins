package fr.minebed.hub.ultimate;

import fr.minebed.hub.common.time.DurationParser;
import fr.minebed.hub.common.util.Cooldowns;
import fr.minebed.hub.compat.Teleports;
import fr.minebed.hub.ultimate.util.Locations;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Téléportation « à la manière d'un serveur » : délai d'attente (annulé si le joueur bouge), temps de recharge,
 * dérogation par permission. Utilisée par /spawn, /home, /warp, /tpaccept et /back.
 */
public final class TeleportService {

    public static final String BYPASS = "ultimatecore.teleport.bypass";

    private final Ctx ctx;
    private final Cooldowns cooldowns = new Cooldowns();
    private final Set<UUID> pending = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final List<BiConsumer<Player, Location>> departureListeners = new CopyOnWriteArrayList<BiConsumer<Player, Location>>();

    public TeleportService(Ctx ctx) {
        this.ctx = ctx;
    }

    /**
     * Prévenu juste avant chaque téléportation faite par ce service, avec l'endroit quitté (pour « /back »).
     * On ne s'appuie pas sur {@code PlayerTeleportEvent} pour nos propres téléportations : Folia ne l'émet pas pour
     * {@code teleportAsync} (constaté sur Folia 1.21.11 et 26.1.2 avec les joueurs simulés).
     */
    public void addDepartureListener(BiConsumer<Player, Location> listener) {
        departureListeners.add(listener);
    }

    /**
     * @param success message à envoyer une fois arrivé (avec ses jetons), ou {@code null}
     * @param applyRules {@code false} pour téléporter tout de suite, sans délai ni temps de recharge (commande de l'administrateur)
     */
    public void teleport(final Player player, final Location target, boolean applyRules, final Msg success, final Object... successPairs) {
        teleportThen(player, target, applyRules, null, success, successPairs);
    }

    /** Comme {@link #teleport}, avec une action lancée seulement si le joueur est bien arrivé (temps de recharge d'une commande, par exemple). */
    public void teleportThen(final Player player, final Location target, boolean applyRules, final Runnable afterSuccess, final Msg success, final Object... successPairs) {
        final UUID id = player.getUniqueId();
        boolean bypass = !applyRules || player.hasPermission(BYPASS);
        if (!bypass) {
            long left = cooldowns.remaining(id);
            if (left > 0) {
                ctx.messages.send(player, Msg.TP_COOLDOWN, "time", formatTime(left));
                return;
            }
        }
        if (!pending.add(id)) {
            ctx.messages.send(player, Msg.TP_PENDING);
            return;
        }
        int warmup = bypass ? 0 : Math.max(0, ctx.config().getInt("teleport.warmup-seconds", 3));
        if (warmup == 0) {
            go(player, target, bypass, afterSuccess, success, successPairs);
            return;
        }
        ctx.messages.send(player, Msg.TP_WARMUP, "seconds", String.valueOf(warmup));
        final Location start = player.getLocation();
        final boolean cancelOnMove = ctx.config().getBoolean("teleport.cancel-on-move", true);
        final long[] ticksLeft = {warmup * 20L};
        final Runnable[] step = new Runnable[1];
        step[0] = new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    pending.remove(id);
                    return;
                }
                if (cancelOnMove && !Locations.sameBlock(start, player.getLocation())) {
                    pending.remove(id);
                    ctx.messages.send(player, Msg.TP_CANCELLED_MOVED);
                    return;
                }
                ticksLeft[0] -= 10;
                if (ticksLeft[0] <= 0) {
                    go(player, target, false, afterSuccess, success, successPairs);
                } else {
                    ctx.scheduler.runForEntityLater(player, step[0], new Runnable() {
                        @Override
                        public void run() {
                            pending.remove(id);
                        }
                    }, 10);
                }
            }
        };
        ctx.scheduler.runForEntityLater(player, step[0], new Runnable() {
            @Override
            public void run() {
                pending.remove(id);
            }
        }, 10);
    }

    private void go(final Player player, Location target, final boolean bypass, final Runnable afterSuccess, final Msg success, final Object[] successPairs) {
        final UUID id = player.getUniqueId();
        Location from = player.getLocation();
        for (BiConsumer<Player, Location> listener : departureListeners) {
            listener.accept(player, from);
        }
        Teleports.teleport(ctx.scheduler, player, target, new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(final Boolean ok) {
                pending.remove(id);
                ctx.runOnPlayer(player, new Runnable() {
                    @Override
                    public void run() {
                        if (ok) {
                            if (afterSuccess != null) {
                                afterSuccess.run();
                            }
                            if (!bypass) {
                                long seconds = Math.max(0, ctx.config().getLong("teleport.cooldown-seconds", 5));
                                if (seconds > 0) {
                                    cooldowns.start(id, seconds * 1000L);
                                }
                            }
                            if (success != null) {
                                ctx.messages.send(player, success, successPairs);
                            }
                        } else {
                            ctx.messages.send(player, Msg.TP_FAILED);
                        }
                    }
                });
            }
        });
    }

    public void forget(UUID player) {
        pending.remove(player);
        cooldowns.clear(player);
    }

    private static String formatTime(long millis) {
        return DurationParser.format(millis, "j", "h", "min", "s");
    }
}
