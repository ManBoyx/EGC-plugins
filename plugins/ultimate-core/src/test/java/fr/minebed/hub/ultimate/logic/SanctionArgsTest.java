package fr.minebed.hub.ultimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SanctionArgsTest {

    private static final String NONE = "aucun motif";

    @Test
    void durationThenReason() {
        SanctionArgs a = SanctionArgs.parse(new String[] {"Bob", "10m", "spam", "répété"}, 1, NONE);
        assertFalse(a.permanent());
        assertEquals(600_000L, a.durationMillis());
        assertEquals("spam répété", a.reason());
    }

    @Test
    void noDurationMeansPermanentAndEverythingIsTheReason() {
        SanctionArgs a = SanctionArgs.parse(new String[] {"Bob", "insultes", "graves"}, 1, NONE);
        assertTrue(a.permanent());
        assertEquals("insultes graves", a.reason());
    }

    @Test
    void nothingAfterTheName() {
        SanctionArgs a = SanctionArgs.parse(new String[] {"Bob"}, 1, NONE);
        assertTrue(a.permanent());
        assertEquals(NONE, a.reason());
    }

    @Test
    void durationAloneKeepsTheDefaultReason() {
        SanctionArgs a = SanctionArgs.parse(new String[] {"Bob", "2h"}, 1, NONE);
        assertEquals(7_200_000L, a.durationMillis());
        assertEquals(NONE, a.reason());
    }

    @Test
    void expiryNeverOverflows() {
        SanctionArgs forever = SanctionArgs.parse(new String[] {"Bob"}, 1, NONE);
        assertEquals(SanctionArgs.PERMANENT, forever.expiresAt(1_000));
        SanctionArgs timed = SanctionArgs.parse(new String[] {"Bob", "1h"}, 1, NONE);
        assertEquals(1_000 + 3_600_000L, timed.expiresAt(1_000));
        assertEquals(SanctionArgs.PERMANENT, timed.expiresAt(Long.MAX_VALUE - 10));
    }
}
