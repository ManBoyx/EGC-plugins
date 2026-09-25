package fr.minebed.hub.common.text;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Outils de modération du chat : majuscules abusives, répétitions, mots interdits (même déguisés). */
public final class TextFilters {

    private TextFilters() {
    }

    /** Part de majuscules parmi les lettres, de 0 à 100 (0 s'il n'y a pas de lettre). */
    public static int capsPercent(String message) {
        int letters = 0;
        int upper = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    upper++;
                }
            }
        }
        return letters == 0 ? 0 : (upper * 100) / letters;
    }

    /** Vrai si le message est assez long et trop crié pour être toléré. */
    public static boolean isShouting(String message, int minLetters, int maxPercent) {
        int letters = 0;
        for (int i = 0; i < message.length(); i++) {
            if (Character.isLetter(message.charAt(i))) {
                letters++;
            }
        }
        return letters >= minLetters && capsPercent(message) > maxPercent;
    }

    /** Ressemblance entre deux textes, de 0 (rien en commun) à 1 (identiques), insensible à la casse et aux accents. */
    public static double similarity(String a, String b) {
        String x = squash(a);
        String y = squash(b);
        if (x.isEmpty() && y.isEmpty()) {
            return 1.0;
        }
        int max = Math.max(x.length(), y.length());
        return 1.0 - (double) levenshtein(x, y) / max;
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    private static String squash(String text) {
        return removeAccents(text).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** « salope! » : la ponctuation de fin de phrase ne doit pas être prise pour une lettre déguisée (« ! » → « i »). */
    private static String trimPunctuation(String text) {
        String edge = "!?.,;:'\"()[]{}*-_";
        int start = 0;
        int end = text.length();
        while (start < end && edge.indexOf(text.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && edge.indexOf(text.charAt(end - 1)) >= 0) {
            end--;
        }
        return text.substring(start, end);
    }

    /** Normalise un mot pour le comparer : minuscules, sans accents, « 4 » devient « a », lettres répétées réduites. */
    static String normalizeWord(String word) {
        String lower = trimPunctuation(removeAccents(word).toLowerCase(Locale.ROOT));
        StringBuilder out = new StringBuilder(lower.length());
        char last = 0;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            switch (c) {
                case '0': c = 'o'; break;
                case '1': case '!': case '|': c = 'i'; break;
                case '3': c = 'e'; break;
                case '4': case '@': c = 'a'; break;
                case '5': case '$': c = 's'; break;
                case '7': c = 't'; break;
                default: break;
            }
            if (!Character.isLetterOrDigit(c)) {
                continue;
            }
            if (c != last) {
                out.append(c);
            }
            last = c;
        }
        return out.toString();
    }

    /** Détecte les mots interdits d'une liste, y compris déguisés (« c0nn4rd », « c o n n a r d », « connnnard »). */
    public static final class WordFilter {
        /** Forme normalisée → mot tel que l'administrateur l'a écrit (en minuscules), pour le rendre dans les messages. */
        private final Map<String, String> words = new HashMap<String, String>();

        public WordFilter(Iterable<String> forbidden) {
            for (String word : forbidden) {
                String normalized = normalizeWord(word);
                if (!normalized.isEmpty()) {
                    words.put(normalized, word.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        /** Le premier mot interdit trouvé (tel que configuré), ou {@code null}. */
        public String find(String message) {
            List<String> tokens = new ArrayList<String>();
            for (String raw : message.split("\\s+")) {
                String token = normalizeWord(raw);
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
            for (String token : tokens) {
                if (words.containsKey(token)) {
                    return words.get(token);
                }
            }
            // Lettres séparées par des espaces : « c o n n a r d » → on recolle les suites de lettres isolées.
            StringBuilder glued = new StringBuilder();
            for (String token : tokens) {
                if (token.length() == 1) {
                    glued.append(token);
                } else {
                    String found = glued.length() > 1 ? words.get(normalizeWord(glued.toString())) : null;
                    if (found != null) {
                        return found;
                    }
                    glued.setLength(0);
                }
            }
            return glued.length() > 1 ? words.get(normalizeWord(glued.toString())) : null;
        }
    }
}
