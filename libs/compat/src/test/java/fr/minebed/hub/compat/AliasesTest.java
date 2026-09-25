package fr.minebed.hub.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class AliasesTest {

    @Test
    void materialCandidatesGoBothWays() {
        assertEquals(Arrays.asList("WOODEN_SWORD", "WOOD_SWORD"), Materials.candidates("wooden sword"));
        assertEquals(Arrays.asList("WOOD_SWORD", "WOODEN_SWORD"), Materials.candidates("WOOD_SWORD"));
        assertEquals(Arrays.asList("DIAMOND_SWORD"), Materials.candidates("minecraft:diamond-sword"));
        assertEquals(Collections.emptyList(), Materials.candidates(null));
        assertEquals(Collections.emptyList(), Materials.candidates("  "));
    }

    @Test
    void ambiguousNamesAreNotBridged() {
        assertEquals(Arrays.asList("BRICK"), Materials.candidates("brick"));
        assertEquals(Arrays.asList("NETHER_BRICK"), Materials.candidates("nether_brick"));
        assertEquals(Arrays.asList("SKULL_ITEM"), Materials.candidates("skull_item"));
    }

    @Test
    void materialsResolveAgainstTheCompileTimeApi() {
        // L'API 1.8.8 avec laquelle on compile ne connaît que les anciens noms : les nouveaux doivent y ramener.
        assertEquals(Material.WOOD_SWORD, Materials.resolve("WOODEN_SWORD"));
        assertEquals(Material.DIAMOND_SWORD, Materials.resolve("diamond sword"));
        assertEquals(Material.GOLD_PICKAXE, Materials.resolve("golden_pickaxe"));
        assertNull(Materials.resolve("PAS_UN_MATERIAU"));
        assertNull(Materials.resolve(null));
    }

    @Test
    void enchantCandidatesGoBothWays() {
        assertEquals(Arrays.asList("SHARPNESS", "DAMAGE_ALL"), Enchants.candidates("sharpness"));
        assertEquals(Arrays.asList("DAMAGE_ALL", "SHARPNESS"), Enchants.candidates("damage_all"));
        assertEquals(Arrays.asList("UNBREAKING", "DURABILITY"), Enchants.candidates("minecraft:unbreaking"));
        assertEquals(Arrays.asList("MENDING"), Enchants.candidates("Mending"));
        assertEquals(Collections.emptyList(), Enchants.candidates(""));
    }

    @Test
    void soundCandidatesCoverRenames() {
        assertEquals(Arrays.asList("LEVEL_UP", "ENTITY_PLAYER_LEVELUP"), Sounds.candidates("level_up"));
        assertTrue(Sounds.candidates("entity.player.levelup").contains("LEVEL_UP"));
        assertTrue(Sounds.candidates("ENTITY_ENDERMEN_TELEPORT").contains("ENTITY_ENDERMAN_TELEPORT"));
        assertTrue(Sounds.candidates("block_note_block_pling").contains("NOTE_PLING"));
        assertEquals(Collections.emptyList(), Sounds.candidates(null));
    }

    @Test
    void soundsResolveAgainstTheCompileTimeEnum() {
        assertNotNull(Sounds.resolve("ENTITY_PLAYER_LEVELUP")); // ramené à LEVEL_UP sur l'API 1.8.8
        assertNull(Sounds.resolve("PAS_UN_SON"));
    }
}
