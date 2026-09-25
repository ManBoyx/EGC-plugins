package fr.minebed.hub.common.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Temps de recharge par clé (joueur + action). L'horloge est injectable pour pouvoir tester sans attendre. */
public final class Cooldowns {

    private final Map<Object, Long> expiry = new ConcurrentHashMap<Object, Long>();
    private final LongSupplier clock;

    public Cooldowns() {
        this(System::currentTimeMillis);
    }

    public Cooldowns(LongSupplier clock) {
        this.clock = clock;
    }

    public void start(Object key, long millis) {
        expiry.put(key, clock.getAsLong() + millis);
    }

    /** Millisecondes restantes, 0 si le temps de recharge est terminé (ou n'a jamais commencé). */
    public long remaining(Object key) {
        Long until = expiry.get(key);
        if (until == null) {
            return 0;
        }
        long left = until - clock.getAsLong();
        if (left <= 0) {
            expiry.remove(key, until);
            return 0;
        }
        return left;
    }

    public boolean isActive(Object key) {
        return remaining(key) > 0;
    }

    public void clear(Object key) {
        expiry.remove(key);
    }

    /** Oublie les temps de recharge terminés (à appeler de temps en temps pour ne pas grossir sans fin). */
    public void purge() {
        long now = clock.getAsLong();
        for (Iterator<Map.Entry<Object, Long>> it = expiry.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }
    }

    public int size() {
        return expiry.size();
    }
}
