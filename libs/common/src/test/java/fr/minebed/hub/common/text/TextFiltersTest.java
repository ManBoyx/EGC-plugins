package fr.minebed.hub.common.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TextFiltersTest {

    @Test
    void capsPercentCountsLettersOnly() {
        assertEquals(0, TextFilters.capsPercent("1234 !!"));
        assertEquals(100, TextFilters.capsPercent("BONJOUR 42"));
        assertEquals(50, TextFilters.capsPercent("ABcd"));
    }

    @Test
    void shortShoutsAreTolerated() {
        assertFalse(TextFilters.isShouting("OK", 8, 60));
        assertTrue(TextFilters.isShouting("ARRETEZ DE FAIRE CA", 8, 60));
        assertFalse(TextFilters.isShouting("Bonjour à tous, ça va ?", 8, 60));
    }

    @Test
    void similarityIgnoresCaseAccentsAndPunctuation() {
        assertEquals(1.0, TextFilters.similarity("Où est le spawn ?", "ou est le spawn"), 1e-9);
        assertTrue(TextFilters.similarity("achète mon grade", "achete mon gradee") > 0.9);
        assertTrue(TextFilters.similarity("bonjour", "au revoir") < 0.5);
        assertEquals(1.0, TextFilters.similarity("", "!!"), 1e-9);
    }

    @Test
    void wordFilterFindsPlainAndDisguisedWords() {
        TextFilters.WordFilter filter = new TextFilters.WordFilter(Arrays.asList("connard", "Salope"));
        assertEquals("connard", filter.find("espèce de connard"));
        assertEquals("connard", filter.find("espèce de c0nn4rd"));
        assertEquals("connard", filter.find("connnnnard"));
        assertEquals("connard", filter.find("c o n n a r d"));
        assertEquals("salope", filter.find("SALOPE!"));
    }

    @Test
    void wordFilterDoesNotFlagInnocentText() {
        TextFilters.WordFilter filter = new TextFilters.WordFilter(Arrays.asList("con"));
        assertNull(filter.find("un concombre concourt à Concarneau"));
        assertNotNull(filter.find("quel con"));
        assertNull(filter.find(""));
        assertNull(new TextFilters.WordFilter(Arrays.asList("")).find("bonjour"));
    }
}
