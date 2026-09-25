package fr.minebed.hub.common.time;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lit et écrit des durées à la manière des commandes de serveur : « 1d2h30m », « 90s », « 45 ». */
public final class DurationParser {

    private static final Pattern PART = Pattern.compile("(\\d+)\\s*([smhdwjSMHDWJ]?)");
    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;

    private DurationParser() {
    }

    /**
     * Durée en millisecondes. Unités : s, m, h, d (ou j pour « jour »), w. Un nombre seul compte en secondes.
     * Renvoie « vide » si le texte n'est pas une durée valide.
     */
    public static OptionalLong parseMillis(String text) {
        if (text == null) {
            return OptionalLong.empty();
        }
        String trimmed = text.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return OptionalLong.empty();
        }
        Matcher matcher = PART.matcher(trimmed);
        long total = 0;
        int consumed = 0;
        while (matcher.find()) {
            if (matcher.start() != consumed && !trimmed.substring(consumed, matcher.start()).trim().isEmpty()) {
                return OptionalLong.empty();
            }
            consumed = matcher.end();
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
            long unit;
            switch (matcher.group(2)) {
                case "m": unit = MINUTE; break;
                case "h": unit = HOUR; break;
                case "d": case "j": unit = DAY; break;
                case "w": unit = WEEK; break;
                default: unit = SECOND; break;
            }
            if (amount > Long.MAX_VALUE / unit || total > Long.MAX_VALUE - amount * unit) {
                return OptionalLong.empty();
            }
            total += amount * unit;
        }
        return consumed == trimmed.length() && total > 0 ? OptionalLong.of(total) : OptionalLong.empty();
    }

    /** « 1j 2h 30min 5s » ; les libellés viennent des messages pour pouvoir être traduits. */
    public static String format(long millis, String day, String hour, String minute, String second) {
        long remaining = Math.max(0, millis);
        long days = remaining / DAY;
        remaining %= DAY;
        long hours = remaining / HOUR;
        remaining %= HOUR;
        long minutes = remaining / MINUTE;
        remaining %= MINUTE;
        long seconds = (remaining + SECOND - 1) / SECOND;
        if (seconds == 60) {
            seconds = 0;
            minutes++;
        }
        StringBuilder out = new StringBuilder();
        if (days > 0) {
            out.append(days).append(day).append(' ');
        }
        if (hours > 0) {
            out.append(hours).append(hour).append(' ');
        }
        if (minutes > 0) {
            out.append(minutes).append(minute).append(' ');
        }
        if (seconds > 0 || out.length() == 0) {
            out.append(seconds).append(second).append(' ');
        }
        return out.toString().trim();
    }
}
