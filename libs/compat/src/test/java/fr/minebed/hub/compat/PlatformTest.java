package fr.minebed.hub.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.minebed.hub.common.version.MinecraftVersion;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlatformTest {

    private static Platform detect(String bukkit, String server, String... classes) {
        Set<String> present = new HashSet<String>(Arrays.asList(classes));
        return Platform.detect(present::contains, bukkit, server);
    }

    @Test
    void foliaWinsOverPaper() {
        Platform p = detect("1.21.4-R0.1-SNAPSHOT", "x", "io.papermc.paper.threadedregions.RegionizedServer", "io.papermc.paper.configuration.Configuration");
        assertEquals(Platform.Kind.FOLIA, p.kind());
        assertTrue(p.isFolia());
        assertTrue(p.isPaperFamily());
    }

    @Test
    void paperOldAndNewMarkers() {
        assertEquals(Platform.Kind.PAPER, detect("1.16.5-R0.1-SNAPSHOT", "x", "com.destroystokyo.paper.PaperConfig").kind());
        assertEquals(Platform.Kind.PAPER, detect("1.20.4-R0.1-SNAPSHOT", "x", "io.papermc.paper.configuration.Configuration").kind());
    }

    @Test
    void spigotAndCraftBukkit() {
        assertEquals(Platform.Kind.SPIGOT, detect("1.8.8-R0.1-SNAPSHOT", "x", "org.spigotmc.SpigotConfig").kind());
        Platform bukkit = detect("1.8.8-R0.1-SNAPSHOT", "x");
        assertEquals(Platform.Kind.CRAFTBUKKIT, bukkit.kind());
        assertFalse(bukkit.isPaperFamily());
        assertFalse(bukkit.isFolia());
    }

    @Test
    void hexColorsFromSixteen() {
        assertFalse(detect("1.12.2-R0.1-SNAPSHOT", "x").supportsHexColors());
        assertTrue(detect("1.16.5-R0.1-SNAPSHOT", "x").supportsHexColors());
        assertTrue(detect("1.21.4-R0.1-SNAPSHOT", "x").supportsHexColors());
    }

    @Test
    void unknownVersionIsHandled() {
        Platform p = detect("???", "???");
        assertNull(p.version());
        assertFalse(p.supportsHexColors());
        assertEquals("CraftBukkit ?", p.toString());
    }

    @Test
    void versionIsReadFromServerBanner() {
        assertEquals(new MinecraftVersion(1, 12, 2), detect("???", "git-Paper-1618 (MC: 1.12.2)").version());
        assertEquals("Paper 1.12.2", detect("???", "git-Paper-1618 (MC: 1.12.2)", "com.destroystokyo.paper.PaperConfig").toString());
    }
}
