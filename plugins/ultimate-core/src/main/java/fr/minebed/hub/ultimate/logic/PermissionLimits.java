package fr.minebed.hub.ultimate.logic;

/** Lit un nombre dans des permissions de la forme « ultimatecore.homes.limit.5 » : on garde le plus grand. */
public final class PermissionLimits {

    private PermissionLimits() {
    }

    /**
     * @param nodes    les permissions actives du joueur
     * @param prefix   ex. {@code "ultimatecore.homes.limit."} (avec le point final)
     * @param fallback valeur si aucune permission ne correspond
     */
    public static int highest(Iterable<String> nodes, String prefix, int fallback) {
        int best = -1;
        String lowerPrefix = prefix.toLowerCase(java.util.Locale.ROOT);
        for (String node : nodes) {
            String lower = node.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith(lowerPrefix)) {
                String tail = lower.substring(lowerPrefix.length());
                if (tail.equals("*")) {
                    return Integer.MAX_VALUE;
                }
                try {
                    if (tail.length() <= 9) {
                        best = Math.max(best, Integer.parseInt(tail));
                    }
                } catch (NumberFormatException ignored) {
                    // permission mal écrite : ignorée
                }
            }
        }
        return best < 0 ? fallback : best;
    }

    /** Un nom de maison, de point de passage ou de kit : lettres, chiffres, « _ » et « - », de 1 à 24 caractères. */
    public static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,24}");
    }
}
