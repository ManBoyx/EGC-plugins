package fr.minebed.hub.common.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlaceholdersTest {

    @Test
    void replacesKnownTokens() {
        assertEquals("Léa a reçu 5 pièces", Placeholders.apply("{player} a reçu {amount} pièces", Placeholders.of("player", "Léa", "amount", 5)));
    }

    @Test
    void leavesUnknownTokensAndStrayBraces() {
        assertEquals("{inconnu} et { seul", Placeholders.apply("{inconnu} et { seul", Placeholders.of("player", "x")));
    }

    @Test
    void valuesAreNotReExpanded() {
        assertEquals("{b}", Placeholders.apply("{a}", Placeholders.of("a", "{b}", "b", "boom")));
    }

    @Test
    void oddNumberOfArgumentsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Placeholders.of("seul"));
    }

    @Test
    void nullTemplateGivesEmptyText() {
        assertEquals("", Placeholders.apply(null, Placeholders.of("a", "b")));
    }
}
