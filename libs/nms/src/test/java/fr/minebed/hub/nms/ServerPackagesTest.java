package fr.minebed.hub.nms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerPackagesTest {

    @Test
    void versionedPackagesAreDetected() {
        ServerPackages p = ServerPackages.fromCraftServerClass("org.bukkit.craftbukkit.v1_8_R3.CraftServer");
        assertTrue(p.hasLegacyNms());
        assertEquals("org.bukkit.craftbukkit.v1_8_R3", p.craftPackage());
    }

    @Test
    void unversionedPackagesHaveNoLegacyNms() {
        ServerPackages p = ServerPackages.fromCraftServerClass("org.bukkit.craftbukkit.CraftServer");
        assertFalse(p.hasLegacyNms());
        assertEquals("org.bukkit.craftbukkit", p.craftPackage());
        assertNull(p.nms("EntityPlayer"));
    }

    @Test
    void reflectNeverThrows() {
        assertNull(Reflect.findClass("n.existe.Pas"));
        assertNull(Reflect.findMethod(String.class, "nexistepas"));
        assertNull(Reflect.findField(String.class, "nexistepas"));
        assertNull(Reflect.invoke(null, "x"));
        assertEquals(3, Reflect.invoke(Reflect.findMethod(String.class, "length"), "abc"));
    }
}
