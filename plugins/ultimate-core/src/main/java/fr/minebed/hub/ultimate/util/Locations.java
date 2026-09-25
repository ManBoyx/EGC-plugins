package fr.minebed.hub.ultimate.util;

import fr.minebed.hub.common.util.SavedLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** Passage entre les emplacements de Bukkit et ceux que l'on écrit dans les fichiers. */
public final class Locations {

    private Locations() {
    }

    public static SavedLocation save(Location l) {
        World world = l.getWorld();
        return new SavedLocation(world == null ? "world" : world.getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    /** {@code null} si le monde n'est pas chargé (supprimé, renommé…). */
    public static Location restore(SavedLocation saved) {
        World world = Bukkit.getWorld(saved.world());
        return world == null ? null : new Location(world, saved.x(), saved.y(), saved.z(), saved.yaw(), saved.pitch());
    }

    public static boolean sameBlock(Location a, Location b) {
        return a.getWorld() == b.getWorld() && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }
}
