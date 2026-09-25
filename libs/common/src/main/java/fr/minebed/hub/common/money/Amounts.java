package fr.minebed.hub.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalDouble;

/** Montants saisis par les joueurs (« 1500 », « 1,5k », « 2M ») et leur affichage. */
public final class Amounts {

    private Amounts() {
    }

    /** Montant strictement positif, arrondi au centime ; vide si le texte n'est pas un montant valide. */
    public static OptionalDouble parsePositive(String text) {
        if (text == null) {
            return OptionalDouble.empty();
        }
        String cleaned = text.trim().toLowerCase().replace(',', '.').replace("_", "");
        if (cleaned.isEmpty()) {
            return OptionalDouble.empty();
        }
        double multiplier = 1;
        char last = cleaned.charAt(cleaned.length() - 1);
        if (last == 'k') {
            multiplier = 1_000d;
        } else if (last == 'm') {
            multiplier = 1_000_000d;
        } else if (last == 'b') {
            multiplier = 1_000_000_000d;
        }
        if (multiplier != 1) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (!cleaned.matches("\\d+(\\.\\d+)?")) {
            return OptionalDouble.empty();
        }
        BigDecimal value = new BigDecimal(cleaned).multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP);
        if (value.signum() <= 0 || value.compareTo(BigDecimal.valueOf(1_000_000_000_000d)) > 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(value.doubleValue());
    }

    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** « 1 234,50 » : espace comme séparateur de milliers, virgule pour les centimes, sans dépendre de la langue du serveur. */
    public static String format(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
        String plain = rounded.abs().toPlainString();
        int dot = plain.indexOf('.');
        String integer = dot < 0 ? plain : plain.substring(0, dot);
        String cents = dot < 0 ? "00" : plain.substring(dot + 1);
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < integer.length(); i++) {
            if (i > 0 && (integer.length() - i) % 3 == 0) {
                grouped.append(' ');
            }
            grouped.append(integer.charAt(i));
        }
        return (rounded.signum() < 0 ? "-" : "") + grouped + "," + cents;
    }
}
