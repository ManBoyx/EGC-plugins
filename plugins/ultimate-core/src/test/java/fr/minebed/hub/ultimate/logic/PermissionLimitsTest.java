package fr.minebed.hub.ultimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PermissionLimitsTest {

    private static final String PREFIX = "ultimatecore.homes.limit.";

    @Test
    void keepsTheHighestNumber() {
        assertEquals(10, PermissionLimits.highest(Arrays.asList("ultimatecore.homes.limit.3", "ultimatecore.homes.limit.10", "autre.chose"), PREFIX, 1));
    }

    @Test
    void fallsBackWhenNothingMatches() {
        assertEquals(2, PermissionLimits.highest(Arrays.asList("autre.chose"), PREFIX, 2));
        assertEquals(2, PermissionLimits.highest(Arrays.asList("ultimatecore.homes.limit.abc"), PREFIX, 2));
    }

    @Test
    void wildcardMeansUnlimited() {
        assertEquals(Integer.MAX_VALUE, PermissionLimits.highest(Arrays.asList("ultimatecore.homes.limit.*"), PREFIX, 1));
    }

    @Test
    void ignoresAbsurdlyLongNumbersAndIsCaseInsensitive() {
        assertEquals(1, PermissionLimits.highest(Arrays.asList("ultimatecore.homes.limit.99999999999999"), PREFIX, 1));
        assertEquals(5, PermissionLimits.highest(Arrays.asList("UltimateCore.Homes.Limit.5"), PREFIX, 1));
    }

    @Test
    void zeroIsAValidLimit() {
        assertEquals(0, PermissionLimits.highest(Arrays.asList("ultimatecore.homes.limit.0"), PREFIX, 3));
    }

    @Test
    void nameValidation() {
        assertTrue(PermissionLimits.isValidName("maison_1"));
        assertTrue(PermissionLimits.isValidName("a-b"));
        assertFalse(PermissionLimits.isValidName(""));
        assertFalse(PermissionLimits.isValidName("a b"));
        assertFalse(PermissionLimits.isValidName("é"));
        assertFalse(PermissionLimits.isValidName("../etc"));
        assertFalse(PermissionLimits.isValidName("x123456789012345678901234567890"));
        assertFalse(PermissionLimits.isValidName(null));
    }
}
