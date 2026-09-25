package fr.minebed.hub.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SavedLocationTest {

    @Test
    void roundTrips() {
        SavedLocation l = new SavedLocation("world_nether", 12.5, 64, -300.25, 90f, -12.5f);
        assertEquals("world_nether;12.500;64.000;-300.250;90.000;-12.500", l.format());
        SavedLocation back = SavedLocation.parse(l.format());
        assertEquals("world_nether", back.world());
        assertEquals(12.5, back.x(), 1e-9);
        assertEquals(-300.25, back.z(), 1e-9);
        assertEquals(-12.5f, back.pitch(), 1e-6);
    }

    @Test
    void damagedTextGivesNull() {
        assertNull(SavedLocation.parse(null));
        assertNull(SavedLocation.parse(""));
        assertNull(SavedLocation.parse("world;1;2;3"));
        assertNull(SavedLocation.parse("world;a;2;3;0;0"));
        assertNull(SavedLocation.parse("world;NaN;2;3;0;0"));
        assertNull(SavedLocation.parse(";1;2;3;0;0"));
    }

    @Test
    void constructorRejectsNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new SavedLocation("a;b", 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SavedLocation("w", Double.POSITIVE_INFINITY, 0, 0, 0, 0));
    }
}
