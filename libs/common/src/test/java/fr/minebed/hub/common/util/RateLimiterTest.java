package fr.minebed.hub.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void allowsUpToMaxPerWindow() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(3, 1_000, now::get);
        assertTrue(limiter.tryAcquire("p"));
        assertTrue(limiter.tryAcquire("p"));
        assertTrue(limiter.tryAcquire("p"));
        assertFalse(limiter.tryAcquire("p"));
        assertTrue(limiter.tryAcquire("autre"));
    }

    @Test
    void windowSlides() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(2, 1_000, now::get);
        assertTrue(limiter.tryAcquire("p"));
        now.set(600);
        assertTrue(limiter.tryAcquire("p"));
        assertFalse(limiter.tryAcquire("p"));
        now.set(1_000); // la première action sort de la fenêtre
        assertTrue(limiter.tryAcquire("p"));
        assertFalse(limiter.tryAcquire("p"));
    }

    @Test
    void refusedActionsAreNotCounted() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(1, 1_000, now::get);
        assertTrue(limiter.tryAcquire("p"));
        for (int i = 0; i < 50; i++) {
            assertFalse(limiter.tryAcquire("p"));
        }
        now.set(1_000);
        assertTrue(limiter.tryAcquire("p"));
    }

    @Test
    void rejectsNonsenseSettings() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0, 1_000));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(1, 0));
    }
}
