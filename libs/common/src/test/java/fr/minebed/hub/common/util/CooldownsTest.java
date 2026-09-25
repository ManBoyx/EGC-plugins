package fr.minebed.hub.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CooldownsTest {

    @Test
    void countsDownWithTheClock() {
        AtomicLong now = new AtomicLong(1_000);
        Cooldowns cooldowns = new Cooldowns(now::get);
        assertFalse(cooldowns.isActive("a"));
        cooldowns.start("a", 5_000);
        assertEquals(5_000, cooldowns.remaining("a"));
        now.addAndGet(2_000);
        assertEquals(3_000, cooldowns.remaining("a"));
        now.addAndGet(3_000);
        assertEquals(0, cooldowns.remaining("a"));
        assertEquals(0, cooldowns.size());
    }

    @Test
    void keysAreIndependent() {
        AtomicLong now = new AtomicLong(0);
        Cooldowns cooldowns = new Cooldowns(now::get);
        cooldowns.start("a", 1_000);
        assertTrue(cooldowns.isActive("a"));
        assertFalse(cooldowns.isActive("b"));
        cooldowns.clear("a");
        assertFalse(cooldowns.isActive("a"));
    }

    @Test
    void purgeForgetsFinishedEntries() {
        AtomicLong now = new AtomicLong(0);
        Cooldowns cooldowns = new Cooldowns(now::get);
        cooldowns.start("short", 100);
        cooldowns.start("long", 10_000);
        now.set(500);
        cooldowns.purge();
        assertEquals(1, cooldowns.size());
        assertTrue(cooldowns.isActive("long"));
    }
}
