package fr.minebed.hub.common.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version du jeu (« 1.8.8 », « 1.21.4 »), avec la comparaison qui va avec.
 * Fonctionne aussi avec le nouveau schéma de numérotation de Mojang (« 26.1 » : l'année, puis le numéro de mise à jour).
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {

    private static final Pattern NUMBERS = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern IN_BRACKETS = Pattern.compile("MC:\\s*(\\d+\\.\\d+(?:\\.\\d+)?)");

    private final int major;
    private final int minor;
    private final int patch;

    public MinecraftVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /** Lit le premier numéro de version d'un texte comme « 1.20.4-R0.1-SNAPSHOT » ; {@code null} si rien n'est reconnu. */
    public static MinecraftVersion parse(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = NUMBERS.matcher(text);
        if (!m.find()) {
            return null;
        }
        return new MinecraftVersion(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), m.group(3) == null ? 0 : Integer.parseInt(m.group(3)));
    }

    /**
     * Devine la version d'après ce que le serveur annonce : {@code getBukkitVersion()} (« 1.20.4-R0.1-SNAPSHOT »)
     * ou, à défaut, {@code getVersion()} (« git-Paper-496 (MC: 1.20.4) »).
     */
    public static MinecraftVersion fromServer(String bukkitVersion, String serverVersion) {
        MinecraftVersion fromBukkit = parse(bukkitVersion);
        if (fromBukkit != null) {
            return fromBukkit;
        }
        if (serverVersion != null) {
            Matcher m = IN_BRACKETS.matcher(serverVersion);
            if (m.find()) {
                return parse(m.group(1));
            }
        }
        return null;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public boolean isAtLeast(int wantedMajor, int wantedMinor) {
        return compareTo(new MinecraftVersion(wantedMajor, wantedMinor, 0)) >= 0;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MinecraftVersion && compareTo((MinecraftVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    @Override
    public String toString() {
        return patch == 0 ? major + "." + minor : major + "." + minor + "." + patch;
    }
}
