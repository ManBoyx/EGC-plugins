package fr.minebed.hub.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;

/**
 * Trouve un matériau d'après son nom, que le serveur soit d'avant ou d'après la « grande simplification » de la 1.13
 * (« WOOD_SWORD » est devenu « WOODEN_SWORD »). L'administrateur écrit le nom qu'il connaît, on essaie les équivalents.
 * Limites : les valeurs de données de l'ancien système (« WOOL:14 ») ne sont pas gérées, et les noms qui désignent deux
 * objets différents selon la version (« BRICK », « NETHER_BRICK », « SKULL_ITEM ») ne sont volontairement pas rapprochés.
 */
public final class Materials {

    private static final Map<String, String[]> ALIASES = new HashMap<String, String[]>();

    static {
        pair("WOOD_SWORD", "WOODEN_SWORD");
        pair("WOOD_PICKAXE", "WOODEN_PICKAXE");
        pair("WOOD_AXE", "WOODEN_AXE");
        pair("WOOD_SPADE", "WOODEN_SHOVEL");
        pair("WOOD_HOE", "WOODEN_HOE");
        pair("STONE_SPADE", "STONE_SHOVEL");
        pair("IRON_SPADE", "IRON_SHOVEL");
        pair("DIAMOND_SPADE", "DIAMOND_SHOVEL");
        pair("GOLD_SPADE", "GOLDEN_SHOVEL");
        pair("GOLD_SWORD", "GOLDEN_SWORD");
        pair("GOLD_PICKAXE", "GOLDEN_PICKAXE");
        pair("GOLD_AXE", "GOLDEN_AXE");
        pair("GOLD_HOE", "GOLDEN_HOE");
        pair("GOLD_HELMET", "GOLDEN_HELMET");
        pair("GOLD_CHESTPLATE", "GOLDEN_CHESTPLATE");
        pair("GOLD_LEGGINGS", "GOLDEN_LEGGINGS");
        pair("GOLD_BOOTS", "GOLDEN_BOOTS");
        pair("SULPHUR", "GUNPOWDER");
        pair("EXP_BOTTLE", "EXPERIENCE_BOTTLE");
        pair("WORKBENCH", "CRAFTING_TABLE");
        pair("BOOK_AND_QUILL", "WRITABLE_BOOK");
        pair("RAW_FISH", "COD");
        pair("COOKED_FISH", "COOKED_COD");
        pair("PORK", "PORKCHOP");
        pair("GRILLED_PORK", "COOKED_PORKCHOP");
        pair("CARROT_ITEM", "CARROT");
        pair("POTATO_ITEM", "POTATO");
        pair("SEEDS", "WHEAT_SEEDS");
        pair("WATCH", "CLOCK");
        pair("MELON_BLOCK", "MELON");
        pair("LOG", "OAK_LOG");
        pair("WOOD", "OAK_PLANKS");
        pair("LEAVES", "OAK_LEAVES");
        pair("SAPLING", "OAK_SAPLING");
        pair("SIGN", "OAK_SIGN");
        pair("REDSTONE_TORCH_ON", "REDSTONE_TORCH");
        pair("EYE_OF_ENDER", "ENDER_EYE");
        pair("FIREBALL", "FIRE_CHARGE");
        pair("MOB_SPAWNER", "SPAWNER");
        pair("ENDER_STONE", "END_STONE");
        pair("STAINED_GLASS_PANE", "WHITE_STAINED_GLASS_PANE");
        pair("INK_SACK", "INK_SAC");
        pair("SNOW_BALL", "SNOWBALL");
        pair("STORAGE_MINECART", "CHEST_MINECART");
        pair("POWERED_MINECART", "FURNACE_MINECART");
        pair("EXPLOSIVE_MINECART", "TNT_MINECART");
        pair("IRON_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE");
        pair("GOLD_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE");
        pair("WOOD_BUTTON", "OAK_BUTTON");
        pair("FENCE", "OAK_FENCE");
        pair("TRAP_DOOR", "OAK_TRAPDOOR");
        pair("WOOD_DOOR", "OAK_DOOR");
        pair("WOOD_STAIRS", "OAK_STAIRS");
        pair("WOOD_PLATE", "OAK_PRESSURE_PLATE");
    }

    private Materials() {
    }

    private static void pair(String legacy, String modern) {
        if (legacy.equals(modern)) {
            return;
        }
        ALIASES.put(legacy, new String[] {modern});
        ALIASES.put(modern, new String[] {legacy});
    }

    /** Les noms à essayer, dans l'ordre : celui de l'administrateur d'abord, puis les équivalents connus. */
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
        String[] aliases = ALIASES.get(name);
        if (aliases != null) {
            Collections.addAll(out, aliases);
        }
        return out;
    }

    /** Le matériau correspondant, ou {@code null} s'il n'existe pas sur ce serveur. */
    public static Material resolve(String input) {
        for (String candidate : candidates(input)) {
            Material material = Material.matchMaterial(candidate);
            if (material != null) {
                return material;
            }
        }
        return null;
    }
}
