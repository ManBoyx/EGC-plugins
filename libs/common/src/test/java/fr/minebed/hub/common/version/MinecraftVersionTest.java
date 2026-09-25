package fr.minebed.hub.common.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinecraftVersionTest {

    @Test
    void parsesBukkitVersions() {
        MinecraftVersion v = MinecraftVersion.parse("1.20.4-R0.1-SNAPSHOT");
        assertEquals(1, v.major());
        assertEquals(20, v.minor());
        assertEquals(4, v.patch());
        assertEquals(new MinecraftVersion(1, 8, 0), MinecraftVersion.parse("1.8"));
        assertNull(MinecraftVersion.parse("n'importe quoi"));
        assertNull(MinecraftVersion.parse(null));
    }

    @Test
    void readsServerVersionWhenBukkitVersionIsUseless() {
        assertEquals(new MinecraftVersion(1, 12, 2), MinecraftVersion.fromServer("???", "git-Paper-1618 (MC: 1.12.2)"));
        assertEquals(new MinecraftVersion(1, 21, 4), MinecraftVersion.fromServer("1.21.4-R0.1-SNAPSHOT", "whatever"));
        assertNull(MinecraftVersion.fromServer(null, null));
    }

    @Test
    void comparesNaturally() {
        assertTrue(MinecraftVersion.parse("1.9").compareTo(MinecraftVersion.parse("1.10")) < 0); // pas un tri de texte
        assertTrue(MinecraftVersion.parse("1.16.5").isAtLeast(1, 16));
        assertFalse(MinecraftVersion.parse("1.12.2").isAtLeast(1, 13));
        assertTrue(MinecraftVersion.parse("26.1").isAtLeast(1, 21));
        assertEquals("1.8", new MinecraftVersion(1, 8, 0).toString());
        assertEquals("1.8.8", new MinecraftVersion(1, 8, 8).toString());
    }
}
