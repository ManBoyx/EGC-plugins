package fr.minebed.hub.common.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Remplace les {jetons} d'un texte ; un jeton inconnu est laissé tel quel (plus facile à repérer). */
public final class Placeholders {

    private Placeholders() {
    }

    /** {@code of("player", "Léa", "amount", "5")} */
    public static Map<String, String> of(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Les jetons vont par paires (nom, valeur).");
        }
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
        }
        return map;
    }

    public static String apply(String template, Map<String, String> values) {
        if (template == null || template.isEmpty() || values == null || values.isEmpty()) {
            return template == null ? "" : template;
        }
        StringBuilder out = new StringBuilder(template.length() + 16);
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i + 1);
                if (end > i) {
                    String key = template.substring(i + 1, end);
                    String value = values.get(key);
                    if (value != null) {
                        out.append(value);
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    public static Map<String, String> none() {
        return Collections.emptyMap();
    }
}
