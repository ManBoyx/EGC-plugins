package fr.minebed.hub.common.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Autorise au plus {@code max} actions par fenêtre glissante de {@code windowMillis} pour chaque clé. */
public final class RateLimiter {

    private final int max;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Map<Object, Deque<Long>> events = new ConcurrentHashMap<Object, Deque<Long>>();

    public RateLimiter(int max, long windowMillis) {
        this(max, windowMillis, System::currentTimeMillis);
    }

    public RateLimiter(int max, long windowMillis, LongSupplier clock) {
        if (max < 1 || windowMillis < 1) {
            throw new IllegalArgumentException("max et fenêtre doivent être positifs");
        }
        this.max = max;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** Enregistre une action et dit si elle est autorisée ; une action refusée n'est pas comptée. */
    public boolean tryAcquire(Object key) {
        long now = clock.getAsLong();
        Deque<Long> queue = events.computeIfAbsent(key, k -> new ArrayDeque<Long>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() >= windowMillis) {
                queue.pollFirst();
            }
            if (queue.size() >= max) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }

    public void reset(Object key) {
        events.remove(key);
    }
}
