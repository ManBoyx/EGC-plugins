package fr.minebed.hub.compat;

import fr.minebed.hub.common.version.MinecraftVersion;
import fr.minebed.hub.nms.Reflect;
import java.util.function.Predicate;
import org.bukkit.Bukkit;

/** Ce sur quoi le plugin tourne : Bukkit, Spigot, Paper ou Folia, et quelle version du jeu. */
public final class Platform {

    public enum Kind {
        CRAFTBUKKIT("CraftBukkit"), SPIGOT("Spigot"), PAPER("Paper"), FOLIA("Folia");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static volatile Platform current;

    private final Kind kind;
    private final MinecraftVersion version;

    Platform(Kind kind, MinecraftVersion version) {
        this.kind = kind;
        this.version = version;
    }

    /** Détection pure (testable) : {@code hasClass} dit si une classe existe sur le serveur. */
    public static Platform detect(Predicate<String> hasClass, String bukkitVersion, String serverVersion) {
        Kind kind;
        if (hasClass.test("io.papermc.paper.threadedregions.RegionizedServer")) {
            kind = Kind.FOLIA;
        } else if (hasClass.test("com.destroystokyo.paper.PaperConfig") || hasClass.test("io.papermc.paper.configuration.Configuration")
            || hasClass.test("io.papermc.paper.PaperBootstrap")) {
            kind = Kind.PAPER;
        } else if (hasClass.test("org.spigotmc.SpigotConfig")) {
            kind = Kind.SPIGOT;
        } else {
            kind = Kind.CRAFTBUKKIT;
        }
        return new Platform(kind, MinecraftVersion.fromServer(bukkitVersion, serverVersion));
    }

    /** La plateforme du serveur en cours (calculée une fois). */
    public static Platform current() {
        Platform local = current;
        if (local == null) {
            synchronized (Platform.class) {
                if (current == null) {
                    current = detect(name -> Reflect.findClass(name) != null, Bukkit.getBukkitVersion(), Bukkit.getVersion());
                }
                local = current;
            }
        }
        return local;
    }

    public Kind kind() {
        return kind;
    }

    /** Paper, ou l'un de ses dérivés (Folia compris) : l'API Paper est disponible. */
    public boolean isPaperFamily() {
        return kind == Kind.PAPER || kind == Kind.FOLIA;
    }

    public boolean isFolia() {
        return kind == Kind.FOLIA;
    }

    /** Peut être {@code null} si le serveur ne dit pas sa version. */
    public MinecraftVersion version() {
        return version;
    }

    /** Les couleurs hexadécimales existent depuis la 1.16. */
    public boolean supportsHexColors() {
        return version != null && version.isAtLeast(1, 16);
    }

    @Override
    public String toString() {
        return kind.label() + " " + (version == null ? "?" : version.toString());
    }
}
