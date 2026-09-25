package fr.minebed.hub.common.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class AmountsTest {

    @Test
    void parsesPlainAndSuffixedAmounts() {
        assertEquals(1500.0, Amounts.parsePositive("1500").getAsDouble(), 1e-9);
        assertEquals(12.5, Amounts.parsePositive("12,5").getAsDouble(), 1e-9);
        assertEquals(1500.0, Amounts.parsePositive("1.5k").getAsDouble(), 1e-9);
        assertEquals(2_000_000.0, Amounts.parsePositive("2M").getAsDouble(), 1e-9);
        assertEquals(3_000_000_000.0, Amounts.parsePositive("3b").getAsDouble(), 1e-9);
        assertEquals(0.01, Amounts.parsePositive("0.005").getAsDouble(), 1e-9);
    }

    @Test
    void rejectsNegativeZeroAndGarbage() {
        assertFalse(Amounts.parsePositive("-5").isPresent());
        assertFalse(Amounts.parsePositive("0").isPresent());
        assertFalse(Amounts.parsePositive("NaN").isPresent());
        assertFalse(Amounts.parsePositive("Infinity").isPresent());
        assertFalse(Amounts.parsePositive("1e9").isPresent());
        assertFalse(Amounts.parsePositive("abc").isPresent());
        assertFalse(Amounts.parsePositive("").isPresent());
        assertFalse(Amounts.parsePositive(null).isPresent());
        assertFalse(Amounts.parsePositive("999999999999999999999").isPresent());
    }

    @Test
    void formatsWithSpacesAndComma() {
        assertEquals("1 234,50", Amounts.format(1234.5));
        assertEquals("0,00", Amounts.format(0));
        assertEquals("1 000 000,00", Amounts.format(1_000_000));
        assertEquals("-12,34", Amounts.format(-12.34));
        assertEquals("999,99", Amounts.format(999.994));
    }

    @Test
    void roundsToCents() {
        assertEquals(0.3, Amounts.round(0.1 + 0.2), 1e-12);
    }
}
