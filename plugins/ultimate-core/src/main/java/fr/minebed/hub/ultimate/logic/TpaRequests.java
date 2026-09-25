package fr.minebed.hub.ultimate.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Demandes de téléportation entre joueurs (« /tpa »), qui expirent d'elles-mêmes. */
public final class TpaRequests {

    /** Qui va vers qui. */
    public enum Direction { REQUESTER_TO_TARGET, TARGET_TO_REQUESTER }

    public static final class Request {
        private final UUID from;
        private final UUID to;
        private final Direction direction;
        private final long expiresAt;

        Request(UUID from, UUID to, Direction direction, long expiresAt) {
            this.from = from;
            this.to = to;
            this.direction = direction;
            this.expiresAt = expiresAt;
        }

        public UUID from() {
            return from;
        }

        public UUID to() {
            return to;
        }

        public Direction direction() {
            return direction;
        }
    }

    private final List<Request> requests = new ArrayList<Request>();
    private final long lifetimeMillis;
    private final LongSupplier clock;

    public TpaRequests(long lifetimeMillis, LongSupplier clock) {
        this.lifetimeMillis = lifetimeMillis;
        this.clock = clock;
    }

    /** Crée une demande ; une ancienne demande du même joueur vers la même cible est remplacée. */
    public synchronized Request create(UUID from, UUID to, Direction direction) {
        purge();
        for (Iterator<Request> it = requests.iterator(); it.hasNext(); ) {
            Request r = it.next();
            if (r.from.equals(from) && r.to.equals(to)) {
                it.remove();
            }
        }
        Request created = new Request(from, to, direction, clock.getAsLong() + lifetimeMillis);
        requests.add(created);
        return created;
    }

    /** Prend (et retire) la demande la plus récente reçue par {@code target}, de {@code from} si précisé. */
    public synchronized Request take(UUID target, UUID from) {
        purge();
        for (int i = requests.size() - 1; i >= 0; i--) {
            Request r = requests.get(i);
            if (r.to.equals(target) && (from == null || r.from.equals(from))) {
                requests.remove(i);
                return r;
            }
        }
        return null;
    }

    /** Annule les demandes envoyées par ce joueur ; renvoie leur nombre. */
    public synchronized int cancelFrom(UUID from) {
        purge();
        int before = requests.size();
        for (Iterator<Request> it = requests.iterator(); it.hasNext(); ) {
            if (it.next().from.equals(from)) {
                it.remove();
            }
        }
        return before - requests.size();
    }

    /** Oublie tout ce qui concerne un joueur qui part. */
    public synchronized void forget(UUID player) {
        for (Iterator<Request> it = requests.iterator(); it.hasNext(); ) {
            Request r = it.next();
            if (r.from.equals(player) || r.to.equals(player)) {
                it.remove();
            }
        }
    }

    public synchronized int pendingCount() {
        purge();
        return requests.size();
    }

    private void purge() {
        long now = clock.getAsLong();
        for (Iterator<Request> it = requests.iterator(); it.hasNext(); ) {
            if (it.next().expiresAt <= now) {
                it.remove();
            }
        }
    }
}
