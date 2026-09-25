package fr.minebed.hub.common.util;

import java.util.Locale;

/** Un endroit du monde, sous une forme simple à écrire dans un fichier : « monde;x;y;z;lacet;tangage ». */
public final class SavedLocation {

    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public SavedLocation(String world, double x, double y, double z, float yaw, float pitch) {
        if (world == null || world.isEmpty() || world.indexOf(';') >= 0) {
            throw new IllegalArgumentException("nom de monde invalide");
        }
        if (!finite(x) || !finite(y) || !finite(z) || !finite(yaw) || !finite(pitch)) {
            throw new IllegalArgumentException("coordonnée invalide");
        }
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /** Lit le format de {@link #format()} ; renvoie {@code null} (jamais d'exception) si le texte est abîmé. */
    public static SavedLocation parse(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.split(";", -1);
        if (parts.length != 6) {
            return null;
        }
        try {
            return new SavedLocation(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String format() {
        return world + ";" + number(x) + ";" + number(y) + ";" + number(z) + ";" + number(yaw) + ";" + number(pitch);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    public String world() {
        return world;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    @Override
    public String toString() {
        return format();
    }
}
