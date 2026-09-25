package fr.minebed.hub.common.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduit les codes de couleur écrits par les administrateurs (« &amp;a », « &amp;#ff8800 ») en codes Minecraft.
 * Les couleurs hexadécimales n'existent qu'à partir de la 1.16 : avant, on prend la couleur classique la plus proche.
 */
public final class ColorCodes {

    private static final char SECTION = '§';
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern ANY_CODE = Pattern.compile("(?i)[§&][0-9a-fk-orx]");
    private static final int[] PALETTE = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
    };
    private static final String CODES = "0123456789abcdef";

    private ColorCodes() {
    }

    public static String translate(String input, boolean hexSupported) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = hexSupported ? expand(hex) : String.valueOf(SECTION) + nearest(hex);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        char[] chars = sb.toString().toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789abcdefABCDEFklmnorKLMNOR".indexOf(chars[i + 1]) >= 0) {
                chars[i] = SECTION;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
                i++;
            }
        }
        return new String(chars);
    }

    /** Retire tous les codes de couleur et de mise en forme (avec « § » comme avec « &amp; »). */
    public static String strip(String input) {
        if (input == null) {
            return "";
        }
        String sansHex = HEX.matcher(input).replaceAll("");
        String sansX = sansHex.replaceAll("(?i)§x(§[0-9a-f]){6}", "");
        return ANY_CODE.matcher(sansX).replaceAll("");
    }

    private static String expand(String hex) {
        StringBuilder out = new StringBuilder().append(SECTION).append('x');
        for (char c : hex.toLowerCase().toCharArray()) {
            out.append(SECTION).append(c);
        }
        return out.toString();
    }

    /** La couleur classique (parmi les seize) la plus proche d'une couleur « rrggbb ». */
    static char nearest(String hex) {
        int rgb = Integer.parseInt(hex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            long dr = r - ((PALETTE[i] >> 16) & 0xFF);
            long dg = g - ((PALETTE[i] >> 8) & 0xFF);
            long db = b - (PALETTE[i] & 0xFF);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return CODES.charAt(best);
    }
}
