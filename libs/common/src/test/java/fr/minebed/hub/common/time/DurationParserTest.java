package fr.minebed.hub.common.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class DurationParserTest {

    @Test
    void parsesSingleUnits() {
        assertEquals(30_000L, DurationParser.parseMillis("30s").getAsLong());
        assertEquals(5 * 60_000L, DurationParser.parseMillis("5m").getAsLong());
        assertEquals(3_600_000L, DurationParser.parseMillis("1h").getAsLong());
        assertEquals(86_400_000L, DurationParser.parseMillis("1d").getAsLong());
        assertEquals(86_400_000L, DurationParser.parseMillis("1j").getAsLong());
        assertEquals(7 * 86_400_000L, DurationParser.parseMillis("1w").getAsLong());
    }

    @Test
    void parsesCombinationsAndBareNumbers() {
        assertEquals(86_400_000L + 2 * 3_600_000L + 30 * 60_000L, DurationParser.parseMillis("1d2h30m").getAsLong());
        assertEquals(90_000L, DurationParser.parseMillis("1m 30s").getAsLong());
        assertEquals(45_000L, DurationParser.parseMillis("45").getAsLong());
        assertEquals(3_600_000L, DurationParser.parseMillis(" 1H ").getAsLong());
    }

    @Test
    void rejectsGarbage() {
        assertFalse(DurationParser.parseMillis(null).isPresent());
        assertFalse(DurationParser.parseMillis("").isPresent());
        assertFalse(DurationParser.parseMillis("abc").isPresent());
        assertFalse(DurationParser.parseMillis("5x").isPresent());
        assertFalse(DurationParser.parseMillis("-5m").isPresent());
        assertFalse(DurationParser.parseMillis("0s").isPresent());
        assertFalse(DurationParser.parseMillis("99999999999999999999d").isPresent());
        assertFalse(DurationParser.parseMillis("9999999999999w").isPresent());
    }

    @Test
    void formatsWithGivenLabels() {
        assertEquals("1j 2h 30min 5s", DurationParser.format(86_400_000L + 2 * 3_600_000L + 30 * 60_000L + 5_000L, "j", "h", "min", "s"));
        assertEquals("45s", DurationParser.format(45_000L, "j", "h", "min", "s"));
        assertEquals("0s", DurationParser.format(0, "j", "h", "min", "s"));
        assertEquals("1min", DurationParser.format(59_001L, "j", "h", "min", "s"));
        assertEquals("1s", DurationParser.format(1, "j", "h", "min", "s"));
    }
}
