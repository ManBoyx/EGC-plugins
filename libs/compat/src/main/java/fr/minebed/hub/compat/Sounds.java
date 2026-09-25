package fr.minebed.hub.compat;

import fr.minebed.hub.nms.Reflect;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Sons : les noms de l'énumération {@code Sound} ont été totalement changés à la 1.9, puis {@code Sound} n'est plus une
 * énumération depuis la 1.21 (ce qui casserait tout appel direct à {@code Sound.valueOf}). On cherche donc par réflexion
 * parmi une liste de noms équivalents, et un son introuvable ne fait jamais d'erreur : il est simplement muet.
 */
public final class Sounds {

    private static final Map<String, String[]> ALIASES = new HashMap<String, String[]>();
    private static final Map<String, Object> CACHE = new HashMap<String, Object>();
    private static final Object MISSING = new Object();

    static {
        alias("LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
        alias("CLICK", "UI_BUTTON_CLICK");
        alias("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP");
        alias("ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT", "ENTITY_ENDERMAN_TELEPORT");
        alias("NOTE_PLING", "BLOCK_NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING");
        alias("VILLAGER_NO", "ENTITY_VILLAGER_NO");
        alias("VILLAGER_YES", "ENTITY_VILLAGER_YES");
        alias("ANVIL_LAND", "BLOCK_ANVIL_LAND");
        alias("ITEM_PICKUP", "ENTITY_ITEM_PICKUP");
        alias("CHEST_OPEN", "BLOCK_CHEST_OPEN");
        alias("EXPLODE", "ENTITY_GENERIC_EXPLODE");
    }

    private Sounds() {
    }

    private static void alias(String legacy, String... modern) {
        ALIASES.put(legacy, modern);
        for (String m : modern) {
            List<String> others = new ArrayList<String>();
            others.add(legacy);
            for (String o : modern) {
                if (!o.equals(m)) {
                    others.add(o);
                }
            }
            ALIASES.put(m, others.toArray(new String[0]));
        }
    }

    public static List<String> candidates(String input) {
        if (input == null) {
            return Collections.emptyList();
        }
        String name = input.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_').replace('.', '_');
        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }
        if (name.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>(3);
        out.add(name);
        String[] more = ALIASES.get(name);
        if (more != null) {
            Collections.addAll(out, more);
        }
        return out;
    }

    /** Le son (un objet {@code org.bukkit.Sound}), ou {@code null}. */
    public static synchronized Object resolve(String input) {
        for (String candidate : candidates(input)) {
            Object cached = CACHE.get(candidate);
            if (cached == null) {
                cached = lookup(candidate);
                CACHE.put(candidate, cached == null ? MISSING : cached);
            }
            if (cached != null && cached != MISSING) {
                return cached;
            }
        }
        return null;
    }

    private static Object lookup(String constantName) {
        Class<?> soundType = Reflect.findClass("org.bukkit.Sound");
        if (soundType == null) {
            return null;
        }
        if (soundType.isEnum()) {
            for (Object constant : soundType.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals(constantName)) {
                    return constant;
                }
            }
            return null;
        }
        // Depuis la 1.21 : constantes statiques de l'interface, de même nom.
        try {
            return soundType.getField(constantName).get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Joue un son au joueur ; ne fait rien (sans erreur) si le son n'existe pas sur ce serveur. */
    public static boolean play(Player player, String name, float volume, float pitch) {
        Object sound = resolve(name);
        if (sound == null) {
            return false;
        }
        Class<?> soundType = Reflect.findClass("org.bukkit.Sound");
        Method method = Reflect.findMethod(Player.class, "playSound", Location.class, soundType, float.class, float.class);
        if (method == null) {
            return false;
        }
        try {
            method.invoke(player, player.getLocation(), sound, volume, pitch);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
