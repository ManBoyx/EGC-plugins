package fr.minebed.hub.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;

/**
 * Trouve un enchantement d'après son nom : les anciens noms (« DAMAGE_ALL ») et les nouveaux (« SHARPNESS », ou
 * « minecraft:sharpness »). Depuis la 1.21 {@link Enchantment} n'est plus une classe mais une interface : ses méthodes
 * statiques ne peuvent donc pas être appelées directement depuis un code compilé pour la 1.8, d'où la réflexion.
 */
public final class Enchants {

    private static final Map<String, String> ALIASES = new HashMap<String, String>();

    static {
        pair("DAMAGE_ALL", "SHARPNESS");
        pair("DAMAGE_UNDEAD", "SMITE");
        pair("DAMAGE_ARTHROPODS", "BANE_OF_ARTHROPODS");
        pair("PROTECTION_ENVIRONMENTAL", "PROTECTION");
        pair("PROTECTION_FIRE", "FIRE_PROTECTION");
        pair("PROTECTION_FALL", "FEATHER_FALLING");
        pair("PROTECTION_EXPLOSIONS", "BLAST_PROTECTION");
        pair("PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION");
        pair("DURABILITY", "UNBREAKING");
        pair("DIG_SPEED", "EFFICIENCY");
        pair("LOOT_BONUS_BLOCKS", "FORTUNE");
        pair("LOOT_BONUS_MOBS", "LOOTING");
        pair("ARROW_DAMAGE", "POWER");
        pair("ARROW_KNOCKBACK", "PUNCH");
        pair("ARROW_FIRE", "FLAME");
        pair("ARROW_INFINITE", "INFINITY");
        pair("WATER_WORKER", "AQUA_AFFINITY");
        pair("OXYGEN", "RESPIRATION");
        pair("LUCK", "LUCK_OF_THE_SEA");
    }

    private Enchants() {
    }

    private static void pair(String legacy, String modern) {
        ALIASES.put(legacy, modern);
        ALIASES.put(modern, legacy);
    }

    /** Les noms à essayer (en majuscules avec « _ ») : celui de l'administrateur, puis l'équivalent. */
    public static List<String> candidates(String input) {
        if (input == null) {
            return Collections.emptyList();
        }
        String name = input.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }
        if (name.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>(2);
        out.add(name);
        String other = ALIASES.get(name);
        if (other != null) {
            out.add(other);
        }
        return out;
    }

    /** L'enchantement correspondant, ou {@code null}. */
    public static Enchantment resolve(String input) {
        for (String candidate : candidates(input)) {
            Object found = Reflection.callStatic("org.bukkit.enchantments.Enchantment", "getByName", new Class<?>[] {String.class}, candidate);
            if (found instanceof Enchantment) {
                return (Enchantment) found;
            }
            found = byKey(candidate.toLowerCase(Locale.ROOT));
            if (found instanceof Enchantment) {
                return (Enchantment) found;
            }
        }
        return null;
    }

    /** {@code Enchantment.getByKey(NamespacedKey.minecraft(nom))}, qui n'existe que depuis la 1.13. */
    private static Object byKey(String lowerName) {
        Object key = Reflection.callStatic("org.bukkit.NamespacedKey", "minecraft", new Class<?>[] {String.class}, lowerName);
        if (key == null) {
            return null;
        }
        Class<?> keyType = fr.minebed.hub.nms.Reflect.findClass("org.bukkit.NamespacedKey");
        return Reflection.callStatic("org.bukkit.enchantments.Enchantment", "getByKey", new Class<?>[] {keyType}, key);
    }
}
